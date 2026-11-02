// Wordkeep's real Android screens for the Play listing. The storeCapture build seeds fictional notes on first launch
// (app/src/storeCapture); StoreCaptureActivity takes the palette and theme as intent extras, so the dark scenes are
// the app's own dark theme. `npx screenshot-studio recapture` builds the storeCapture flavour, runs this for every
// device size, and exports. Alone, after ./gradlew :app:assembleFlavorDefaultStoreCapture:
//   npx screenshot-studio tour [--profile phone] [--locale de-DE] [--only home,editor]

/** Start the app on the amber palette (seeding the notebook when its data was cleared). */
const open = (d, extras = {}, clear = false) => d.launch({clear, extras: {palette: 'amber', ...extras}});

/** @type {import('screenshot-studio').Tour} */
export default {
  app: 'com.bernaferrari.wordkeep.storecapture',
  activity: 'com.bernaferrari.wordkeep.storecapture.StoreCaptureActivity',
  install: '../app/build/outputs/apk/flavorDefault/storeCapture/app-flavorDefault-storeCapture.apk',
  start() {},  // every scene that needs a fresh start opens the app itself
  scenes: {
    light: {
      // A fresh install shows the real intro; the storage choice is the local-first promise.
      async storage(d) { await d.launch({clear: true, extras: {seed: false}}); await d.tap('Get started'); },
      async home(d) { await open(d, {}, true); await open(d); },  // the first start seeds the fictional notebook
      editor: d => d.tap('Lisbon in May'),
      preview: d => d.tap('Preview'),
      color: d => d.tap('Color'),
      async outline(d) { await d.back(); await d.tap('More options'); await d.tap('Document Outline'); },
      async todo(d) {
        await d.back();
        await d.back();
        if (d.profile !== 'phone') await d.back();  // tablets show the editor as a dialog: make sure it is closed
        await open(d);
        await d.tap('todo');
        await d.tap('Preview');
      },
      async journal(d) { await open(d); await d.tap('Journal'); },
      // Search runs through every note, folders included, and marks each match.
      async search(d) { await open(d); await d.tap('Search notes'); await d.type('coffee'); await d.back(); },  // back hides the keyboard
      // The same notebook as a list: real file names, extensions and folders.
      async files(d) { await open(d); await d.tap('Switch to list view'); },
      async settings(d) { await open(d); await d.tap('Settings'); },
      dark: d => open(d, {theme: 'dark'}),
      'dark-editor': d => d.tap('Lisbon in May', {key: 'dark|Lisbon in May'}),
    },
  },
};
