#!/bin/bash
#
# /*
#  * Copyright (c) 2021 Mohamed Tamer
#  *
#  * Permission is hereby granted, free of charge, to any person obtaining
#  * a copy of this software and associated documentation files (the
#  * "Software"), to deal in the Software without restriction, including
#  * without limitation the rights to use, copy, modify, merge, publish,
#  * distribute, sublicense, and/or sell copies of the Software, and to
#  * permit persons to whom the Software is furnished to do so, subject to
#  * the following conditions:
#  *
#  * The above copyright notice and this permission notice shall be
#  * included in all copies or substantial portions of the Software.
#  *
#  * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
#  * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
#  * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
#  * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
#  * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
#  * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
#  * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
#  *
#  */
#
set -e
platform="unknown"
appdata=""
function resolvePlatform() {
  case "$OSTYPE" in
  linux*) platform="linux" ;;
  msys*) platform="windows" ;;
  *) echo "The platform type: $OSTYPE couldn't be resolved as a valid platform!" && exit 1 ;;
  esac
}
function resolveAppDataFolder() {
  if [ $platform == "windows" ]; then
    appdata="$APPDATA/dependencies-resolver"
  elif [ $platform = "MAC" ]; then
    appdata="$HOME/Library/Application/dependencies-resolver"
  elif [ $platform == "linux" ]; then
    appdata="$HOME/.dependencies-resolver"
  fi
}
# resolve the AppData folder
resolvePlatform
resolveAppDataFolder
if [ ! -d "$appdata" ]; then
  mkdir "$appdata"
fi
if [ ! -d "$appdata/gui" ]; then
  mkdir "$appdata/gui"
  echo "Downloading dependencies resolver GUI Zip file.."
  # download the file
  curl -L https://github.com/MohamedTamer94/dependencies-resolver/raw/dev/gui.zip -# >"$appdata"/gui/gui.zip
  echo "Success!"
  # unzip files
  echo "Unzipping Files.."
  unzip -q -o "$appdata"/gui/gui.zip
  echo "Success!"
fi
# execute the JAR file
echo "Executing Program.."
java -jar dependencies-resolver.jar
echo "Success!"
