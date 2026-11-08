#!/usr/bin/env bash
# Seeds the Gradle wrapper cache from a mirror when the official Gradle CDN is
# unreachable. gradle-wrapper.properties pins distributionSha256Sum, so the
# wrapper still verifies the archive before unpacking it.
set -euo pipefail

props="${1:-gradle/wrapper/gradle-wrapper.properties}"
url=$(sed -n 's/^distributionUrl=//p' "$props" | sed 's/\\//g')
version=$(basename "${url%.zip}")

if curl -fsIL --max-time 20 "$url" > /dev/null; then
  echo "Gradle distribution is reachable at $url"
  exit 0
fi

hash=$(python3 - "$url" <<'PY'
import hashlib
import sys

n = int.from_bytes(hashlib.md5(sys.argv[1].encode()).digest(), "big")
digits = "0123456789abcdefghijklmnopqrstuvwxyz"
out = ""
while n:
    out = digits[n % 36] + out
    n //= 36
print(out)
PY
)

dest="${GRADLE_USER_HOME:-$HOME/.gradle}/wrapper/dists/$version/$hash"
mkdir -p "$dest"
echo "Official Gradle CDN unreachable; seeding $version into $dest"

for mirror in \
  "https://mirrors.huaweicloud.com/gradle/$version.zip" \
  "https://mirrors.cloud.tencent.com/gradle/$version.zip"; do
  if curl -fL --retry 2 --retry-delay 5 -o "$dest/$version.zip" "$mirror"; then
    echo "Seeded Gradle distribution from $mirror"
    exit 0
  fi
  rm -f "$dest/$version.zip"
done

echo "Could not obtain the Gradle distribution." >&2
exit 1
