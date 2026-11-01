# Wordkeep · Play listing

Built with [Screenshot Studio](https://github.com/bernaferrari/screenshot-studio) (`npx screenshot-studio`, run from the repository root). Read its [playbook](https://github.com/bernaferrari/screenshot-studio/blob/main/docs/playbook.md) first.

| Change | File |
| --- | --- |
| Cards, order, captures | `gallery.json` |
| Words per language | `<locale>/copy.json`, `<locale>/listing.json` |
| Look | `theme.css`, `theme.js` |
| Canvas and capture sizes | `profiles.json` |
| Scene tour | `capture-emulator.py` (build and runs: `studio.json` → `capture`) |

```sh
npx screenshot-studio recapture                  # build, capture every size on emulator-5578, export
npx screenshot-studio recapture en-US --profile phone --no-build --no-export   # the scene tour alone
npx screenshot-studio studio                     # editor
npx screenshot-studio export --locale en-US
```

The README's notebook spreads use the same real phone and tablet captures as the store listing, with separate `readme` layouts in `gallery.json`. Edit their titles, labels, and source scenes there; `theme.css` and `theme.js` control the paper, highlighter, and frames. To regenerate only the README images, run:

```sh
npx screenshot-studio export --locale en-US --kind readme --strict
```

The original phone and tablet store artwork has its own gallery entries and can still be exported independently.

## Play staging

See [the release guide](../docs/PLAY_STORE.md). Run `node store-listing/render-feature.cjs` to render the 1024 × 500 feature graphic, then `node store-listing/prepare-play.mjs` to stage validated translated text and English images. Set `SHARP_MODULE` if sharp is installed outside this project. Staging never uploads.
