#!/usr/bin/env bash
#
# Copyright 2026 the original author or authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# Builds the APT repository of the .deb files in <debs>, into <out>: the pool, the package index
# of the stable suite, and its Release file, signed with the OpenPGP key in GPG_SECRET_KEY
# (armored, with GPG_PASSPHRASE) when there are packages. GPG_PUBLIC_KEY (armored) goes to
# <out>/tib.gpg as the keyring that apt sources name with signed-by.
set -euo pipefail

debs="${1:?usage: build-apt-repo.sh <debs> <out>}"
out="${2:?usage: build-apt-repo.sh <debs> <out>}"
pool="pool/main/t/tib"
suite="dists/stable"
component="main/binary-amd64"

rm -rf "$out"
mkdir -p "$out/$pool" "$out/$suite/$component"

if [[ -n "${GPG_PUBLIC_KEY:-}" ]]; then
  gpg --dearmor <<<"$GPG_PUBLIC_KEY" >"$out/tib.gpg"
fi

shopt -s nullglob
files=("$debs"/*.deb)
if ((${#files[@]} == 0)); then
  echo "no packages: the repository has an empty index"
fi

# The index: the control fields of each package, and where it is and what it hashes to.
for deb in "${files[@]}"; do
  name="$(basename "$deb")"
  cp "$deb" "$out/$pool/$name"
  dpkg-deb --field "$deb"
  echo "Filename: $pool/$name"
  echo "Size: $(stat -c %s "$deb")"
  echo "MD5sum: $(md5sum "$deb" | cut -d' ' -f1)"
  echo "SHA1: $(sha1sum "$deb" | cut -d' ' -f1)"
  echo "SHA256: $(sha256sum "$deb" | cut -d' ' -f1)"
  echo
done >"$out/$suite/$component/Packages"
gzip -9 -n -k "$out/$suite/$component/Packages"

# The Release file of the suite: what it holds and the hashes of its index.
{
  cat <<RELEASE
Origin: tib
Label: tib
Suite: stable
Codename: stable
Architectures: amd64
Components: main
Description: tib, the idempotent boilerplate tool
Date: $(date -Ru)
RELEASE
  for hash in MD5Sum:md5sum SHA1:sha1sum SHA256:sha256sum; do
    echo "${hash%%:*}:"
    for index in "$out/$suite/$component"/Packages*; do
      printf ' %s %16d %s\n' "$("${hash##*:}" "$index" | cut -d' ' -f1)" \
        "$(stat -c %s "$index")" "$component/$(basename "$index")"
    done
  done
} >"$out/$suite/Release"

if ((${#files[@]} == 0)); then
  exit 0
fi
if [[ -z "${GPG_SECRET_KEY:-}" ]]; then
  echo "GPG_SECRET_KEY is not set: the repository has packages, so its Release must be signed" >&2
  exit 1
fi

# Signed: InRelease for current apt, Release.gpg for older ones. The key lives only in this run.
export GNUPGHOME
GNUPGHOME="$(mktemp -d)"
trap 'rm -rf "$GNUPGHOME"' EXIT
gpg --batch --import <<<"$GPG_SECRET_KEY"
sign=(gpg --batch --yes --pinentry-mode loopback --passphrase "${GPG_PASSPHRASE:-}" --digest-algo SHA256)
"${sign[@]}" --clearsign -o "$out/$suite/InRelease" "$out/$suite/Release"
"${sign[@]}" --detach-sign --armor -o "$out/$suite/Release.gpg" "$out/$suite/Release"
