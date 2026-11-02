// Wordkeep's real Android screens for the Play listing. The storeCapture build seeds fictional notes on first launch
// (app/src/storeCapture); StoreCaptureActivity takes the palette and theme as intent extras, so the dark scenes are
// the app's own dark theme. `npx screenshot-studio recapture` builds the storeCapture flavour, runs this for every
// device size, and exports. Alone, after ./gradlew :app:assembleFlavorDefaultStoreCapture:
//   npx screenshot-studio tour [--profile phone] [--locale de-DE] [--only home,editor]

const palette = 'amber';
const tap = (d, label, key) => d.tap(label, {key, attempts: 6});
const launch = (d, extras = {}, clear = false) => d.launch({clear, extras: {palette, ...extras}});

/** @type {import('screenshot-studio').Tour} */
export default {
  app: 'com.bernaferrari.wordkeep.storecapture',
  activity: 'com.bernaferrari.wordkeep.storecapture.StoreCaptureActivity',
  install: '../app/build/outputs/apk/flavorDefault/storeCapture/app-flavorDefault-storeCapture.apk',
  settle: 1000,
  start() {},  // every scene that needs a fresh start launches itself
  scenes: {
    light: {
      // A fresh install shows the real intro; the storage choice is the local-first promise.
      async storage(d) { await d.launch({clear: true, extras: {seed: false}}); await tap(d, 'Get started'); },
      async home(d) { await launch(d, {}, true); await launch(d); },  // the first launch seeds the fictional notebook
      editor: d => tap(d, 'Lisbon in May'),
      preview: d => tap(d, 'Preview'),
      color: d => tap(d, 'Color'),
      async outline(d) { await d.back(1000); await tap(d, 'More options'); await tap(d, 'Document Outline'); },
      async todo(d) {
        await d.back(1000);
        await d.back(1000);
        if (d.profile !== 'phone') await d.back(1000);  // tablets show the editor as a dialog: make sure it is closed
        await launch(d);
        await tap(d, 'todo');
        await tap(d, 'Preview');
      },
      async journal(d) { await launch(d); await tap(d, 'Journal'); },
      // Search runs through every note, folders included, and marks each match.
      async search(d) {
        await launch(d);
        await tap(d, 'Search notes');
        await d.type('coffee');
        await d.sleep(1500);
        await d.back(1000);  // hide the keyboard; the results stay
      },
      // The same notebook as a list: real file names, extensions and folders.
      async files(d) { await launch(d); await tap(d, 'Switch to list view'); },
      async settings(d) { await launch(d); await tap(d, 'Settings'); },
      dark: d => launch(d, {theme: 'dark'}),
      'dark-editor': d => tap(d, 'Lisbon in May', 'dark|Lisbon in May'),
    },
  },
};
