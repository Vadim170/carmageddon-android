# Carmageddon for Android

Native Android build of **Carmageddon** and **Carmageddon Splat Pack** (1997), no emulation.
The game code is [dethrace](https://github.com/dethrace-labs/dethrace), a reverse-engineered C
reimplementation of the original executable, running on SDL2 on the phone's CPU.

**This repository contains no game files.** You need your own copy of
[Carmageddon Max Pack from GOG](https://www.gog.com/game/carmageddon_max_pack); the build takes
the data from its installer. Do not share APKs built with it: they contain the game.

## What you get

- Two launcher icons, *Carmageddon* and *Carmageddon Splat Pack*, each starting its game directly.
- Everything on screen is the original game: intro and cutscenes, menus, fonts, the hand cursor.
  Menus are used by touch, as with the mouse.
- In races, touch buttons sit in the side bars left free by the 4:3 picture: steering, accelerate,
  brake/reverse, handbrake, pause, map, repair, camera and recovery. They are drawn at runtime from
  the game's own replay-panel buttons and icons, and they follow key remapping done in the game's
  options screen.
- Back button / gesture acts as ESC; a keyboard button in menus brings up the soft keyboard for names.
- The data is packed into the APK and unpacked once on first launch (about 20 s), behind the
  game's own loading screen.

Phone requirements: 64-bit ARM (arm64-v8a), Android 7.0 or newer, about 800 MB free
(the APK is ~290 MB and the game unpacks ~500 MB next to it).

## Building, step by step

Works on macOS and Linux.

### 1. Get the game installer

Buy *Carmageddon Max Pack* on GOG. In your GOG library open the game, choose
*Download offline backup game installers* and download the Windows installer
`setup_carmageddon_max_pack_1.0_gog_(77629).exe` (one ~300 MB file).
A game installed through GOG Galaxy is not enough: the build reads the offline installer.

Put it anywhere; the repository root is fine (`*.exe` is ignored by git).

### 2. Install the tools

**macOS** (with [Homebrew](https://brew.sh)):

```sh
brew install innoextract openjdk@17
```

Python 3 and `bsdtar` come with macOS.

**Debian / Ubuntu:**

```sh
sudo apt install git python3 innoextract libarchive-tools openjdk-17-jdk
```

**Android SDK.** Install [Android Studio](https://developer.android.com/studio), or only the
[command-line tools](https://developer.android.com/studio#command-line-tools-only). Then add the
packages the build uses (in Android Studio: *Settings → Languages & Frameworks → Android SDK → SDK Tools*,
tick *Show Package Details*):

```sh
sdkmanager "platform-tools" "platforms;android-35" "build-tools;35.0.0" \
           "ndk;29.0.13599879" "cmake;3.31.6"
```

Tell Gradle where the SDK is, either with an environment variable

```sh
export ANDROID_HOME="$HOME/Library/Android/sdk"   # macOS default; Linux: $HOME/Android/Sdk
```

or with a file `android/local.properties` containing `sdk.dir=/path/to/Android/sdk`.

### 3. Get the code

The game engine and SDL are git submodules, so clone recursively:

```sh
git clone --recursive https://github.com/Vadim170/carmageddon-android.git
cd carmageddon-android
```

(Already cloned without `--recursive`? Run `git submodule update --init --recursive`.)

### 4. Unpack the game data

```sh
tools/prepare-data.sh "setup_carmageddon_max_pack_1.0_gog_(77629).exe"
```

This creates `game-data/c1` (Carmageddon) and `game-data/sp` (Splat Pack), about 500 MB,
ignored by git. It only needs to be done once.

### 5. Prepare the phone

1. *Settings → About phone → Software information*: tap *Build number* seven times to enable
   developer options.
2. *Settings → Developer options*: turn on *USB debugging*.
3. Connect the phone by USB and accept the *Allow USB debugging* prompt on it.
4. Check that it is visible:

   ```sh
   adb devices        # adb is in $ANDROID_HOME/platform-tools
   ```

### 6. Build and install

```sh
cd android
./gradlew installRelease
```

The first build compiles the engine and packs the game data and takes a few minutes. The APK
itself is `android/app/build/outputs/apk/release/app-release.apk`; you can also copy it to the
phone and install it from there (allow installing from unknown sources).

Open *Carmageddon* or *Carmageddon Splat Pack* on the phone. The first start unpacks the data
behind the game's loading screen, then the intro plays.

The APK is signed with your local debug key, so updates install over each other only when built
on the same computer; otherwise uninstall the old one first (that also deletes save games).

### Faster iterations

`./gradlew installDebug -PnoGameData` leaves the data out of the APK (a few MB instead of ~290 MB).
Push the data to the phone once instead:

```sh
P=/sdcard/Android/data/com.dethrace.carmageddon/files/games
adb shell mkdir -p $P
adb push ../game-data/c1 ../game-data/sp $P/
```

## Layout

| Path | What |
|---|---|
| `android/` | Gradle project: launcher entry, first-run unpacking, SDL activity |
| `dethrace/` | Submodule: [dethrace fork](https://github.com/Vadim170/dethrace/tree/android), branch `android` — Android build, touch controls (`src/harness/platforms/sdl2_touch.c`) |
| `SDL2/` | Submodule: SDL 2.32.10, built from source with the game |
| `tools/prepare-data.sh` | Unpacks the installer and merges each game's CD image data with its installed files |
| `tools/make-resources.py` | Renders the unpacking screens and launcher icons from the game data at build time |

## Notes

- The GOG release keeps key maps and all cutscenes only on the CD images it mounts in DOSBox;
  `prepare-data.sh` takes them from there. `PATHS.TXT` is dropped so the game does not look for a CD drive.
- Rendering is the original software renderer.

## Legal

Carmageddon is the property of its owners (THQ Nordic); this project is not affiliated with or
endorsed by them. dethrace may only be used with assets from an owned copy of Carmageddon, and its
source is for non-commercial use only — see the dethrace repository for its terms.
