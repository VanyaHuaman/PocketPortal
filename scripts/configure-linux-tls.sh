#!/usr/bin/env bash
set -euo pipefail

tls_port=8443
key_alias="pocketportal"
certificate_validity_days=825
secret_byte_count=32
server_host=""
config_dir="${XDG_CONFIG_HOME:-$HOME/.config}/pocketportal"
config_file="$config_dir/pocketportal.properties"
environment_file="$config_dir/pocketportal.env"
tls_dir="$config_dir/tls"
key_store="$tls_dir/pocketportal.p12"
certificate_file="$tls_dir/pocketportal-ca.pem"

usage() {
  echo "Usage: $0 --host PRIVATE_LAN_IP"
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --host)
      server_host="${2:-}"
      shift 2
      ;;
    --help|-h)
      usage
      exit 0
      ;;
    *)
      echo "Unknown argument: $1" >&2
      usage
      exit 1
      ;;
  esac
done

[[ "$server_host" =~ ^([0-9]{1,3}\.){3}[0-9]{1,3}$ ]] || {
  echo "--host must be the PocketPortal server's private IPv4 address." >&2
  exit 1
}
[[ -f "$config_file" ]] || {
  echo "PocketPortal configuration does not exist: $config_file" >&2
  exit 1
}
[[ ! -e "$key_store" && ! -e "$certificate_file" ]] || {
  echo "TLS files already exist under $tls_dir; refusing to overwrite them." >&2
  exit 1
}
command -v keytool >/dev/null || {
  echo "keytool is required and normally ships with the Java runtime." >&2
  exit 1
}
command -v openssl >/dev/null || {
  echo "openssl is required to generate random credentials." >&2
  exit 1
}

mkdir -p "$tls_dir"
chmod 700 "$tls_dir"
key_store_password="$(openssl rand -hex "$secret_byte_count")"
bridge_token="$(openssl rand -hex "$secret_byte_count")"

keytool \
  -genkeypair \
  -alias "$key_alias" \
  -keyalg EC \
  -groupname secp256r1 \
  -validity "$certificate_validity_days" \
  -dname "CN=$server_host" \
  -ext "SAN=ip:$server_host" \
  -storetype PKCS12 \
  -keystore "$key_store" \
  -storepass "$key_store_password" \
  -keypass "$key_store_password" \
  -noprompt
keytool \
  -exportcert \
  -rfc \
  -alias "$key_alias" \
  -keystore "$key_store" \
  -storepass "$key_store_password" \
  -file "$certificate_file"
chmod 600 "$key_store" "$certificate_file"

update_property() {
  local property_name="$1"
  local property_value="$2"
  local temporary_file
  temporary_file="$(mktemp "$config_dir/.pocketportal-properties-XXXXXX")"
  awk -v name="$property_name" -v value="$property_value" '
    BEGIN { updated = 0 }
    index($0, name "=") == 1 {
      print name "=" value
      updated = 1
      next
    }
    { print }
    END {
      if (!updated) {
        print name "=" value
      }
    }
  ' "$config_file" > "$temporary_file"
  mv "$temporary_file" "$config_file"
}

update_secret() {
  local variable_name="$1"
  local variable_value="$2"
  local temporary_file
  temporary_file="$(mktemp "$config_dir/.pocketportal-environment-XXXXXX")"
  if [[ -f "$environment_file" ]]; then
    awk -v name="$variable_name" '
      index($0, name "=") != 1 { print }
    ' "$environment_file" > "$temporary_file"
  fi
  echo "$variable_name=$variable_value" >> "$temporary_file"
  mv "$temporary_file" "$environment_file"
  chmod 600 "$environment_file"
}

update_property "server.host" "127.0.0.1"
update_property "server.tls.enabled" "true"
update_property "server.tls.host" "$server_host"
update_property "server.tls.port" "$tls_port"
update_property "server.tls.keyStorePath" "$key_store"
update_property "server.tls.keyAlias" "$key_alias"
update_property "android.bridge.enabled" "true"
update_secret "POCKETPORTAL_TLS_KEY_STORE_PASSWORD" "$key_store_password"
update_secret "POCKETPORTAL_TLS_PRIVATE_KEY_PASSWORD" "$key_store_password"
update_secret "POCKETPORTAL_ADB_BRIDGE_TOKEN" "$bridge_token"

echo "PocketPortal TLS and the limited ADB bridge are configured."
echo "Certificate to copy to clients: $certificate_file"
echo "Restart PocketPortal with: systemctl --user restart pocketportal.service"
