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
# Prints the Homebrew formula of tib <version>: the macOS zip of each processor, at its URL with
# its SHA-256. The formula installs the app bundle with its runtime, so that tib needs no Java of
# the user's, and builds the start-up cache where it is installed, as the other packages do.
set -euo pipefail

usage='usage: homebrew-formula.sh <version> <arm64-url> <arm64-sha256> <x64-url> <x64-sha256>'
version="${1:?$usage}"
arm64_url="${2:?$usage}"
arm64_sha256="${3:?$usage}"
x64_url="${4:?$usage}"
x64_sha256="${5:?$usage}"

cat <<EOF
# Written by the release of tib $version; do not edit it here.
class Tib < Formula
  desc "Scaffolding CLI for Java services, on Spring Boot or in plain Java"
  homepage "https://github.com/mryakar/tib-cli"
  version "$version"
  license "Apache-2.0"

  on_arm do
    url "$arm64_url"
    sha256 "$arm64_sha256"
  end
  on_intel do
    url "$x64_url"
    sha256 "$x64_sha256"
  end

  depends_on :macos

  def install
    # Homebrew enters the only directory of an archive, which here is the bundle itself.
    bundle = File.basename(Dir.pwd) == "tib.app" ? "." : "tib.app"
    (libexec/"tib.app").install Dir["#{bundle}/*"]
    bin.write_exec_script libexec/"tib.app/Contents/MacOS/tib"
  end

  def post_install
    system libexec/"tib.app/Contents/app/startup-cache.sh"
  end

  test do
    assert_match version.to_s, shell_output("#{bin}/tib --version")
  end
end
EOF
