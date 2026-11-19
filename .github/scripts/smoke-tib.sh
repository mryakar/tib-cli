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
# Checks an installed or unpacked tib: <tib> runs, the start-up cache is in <app>, the directory
# of its jars, and a service scaffolded under <out> has a Gradle wrapper to run.
set -euo pipefail

usage='usage: smoke-tib.sh <tib> <app> <out>'
tib="${1:?$usage}"
app="${2:?$usage}"
out="${3:?$usage}"

"$tib" --version
if [[ ! -f "$app/tib.aot" ]]; then
  echo "$app has no start-up cache" >&2
  exit 1
fi
"$tib" init service smoke -d "$out/smoke"
if [[ ! -f "$out/smoke/gradlew" ]]; then
  echo "the service has no gradlew" >&2
  exit 1
fi
# Windows has no executable bit to check.
if [[ "$OSTYPE" != msys* && ! -x "$out/smoke/gradlew" ]]; then
  echo "the gradlew of the service is not executable" >&2
  exit 1
fi
