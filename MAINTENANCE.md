# Maintaining the plugin

## Forging a new release

The plugin is released manually, based on a git tag.

A gradle plugin automatically determines the current tag and / or if this
is a snapshot release.

To build the plugin, execute the gradle task `buildPlugin`.

```bash
./gradlew clean buildPlugin
```

The artifact zip can then be found in `build/distrubutions`. This is the
final result which can be uploaded to the JetBrains repository.

The checklist for a new release should be the following:

* ensure the project is currently on the latest commit on the `master` branch
  You can enforce this by pulling and resetting with the `--hard` flag
* make sure there are no staged changes
* prepare the changelog:
  * execute `./prepare-release.sh` to write the changelog to disk
  * manually copy the relevant parts to `CHANGELOG.md`
* commit the changed files (preferrable with a meaningful commit message 
  `Prepare release 0.16.xxx`)
* tag a release (`git tag 0.x.xxx`)
* push the changed code and the tag to the remote (`git push && git push --tags`)
* build the plugin on the tag (`./gradlew clean buildPlugin`)

## Upload to JetBrain Plugin repository

The plugin can be updated in two different ways.

### Manual upload

Upload the produced ZIP file to the JetBrains repository manually

### Semi-automatic upload through gradle

The IntelliJ gradle plugin ships a task to upload the release
automatically. This will include the changelog generated earlier.

Execute the following gradle task:

```bash
IJ_REPO_USERNAME=youruser IJ_REPO_PASSWORD=yourpassword ./gradlew clean buildPlugin publishPlugin
```
