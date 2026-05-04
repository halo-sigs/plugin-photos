## 1. Refactor LazyImage component

- [x] 1.1 Remove hidden `new Image()` preload logic and `onMounted` fetch from `LazyImage.vue`
- [x] 1.2 Add native `loading="lazy"` and `decoding="async"` to the visible `<img>` element
- [x] 1.3 Drive `isLoading` and `error` states from `@load` and `@error` events on the `<img>` element
- [x] 1.4 Keep `#loading` and `#error` slots with identical markup and styling behavior

## 2. Verify integration

- [x] 2.1 Confirm `PhotoGrid.vue` still passes the same props to `LazyImage` with no prop API changes
- [x] 2.2 Run `pnpm lint` and `pnpm type-check` in the `console` directory
- [x] 2.3 Build the frontend with `pnpm build` to ensure no compilation errors

## 3. Validate performance

- [x] 3.1 Open the console photo grid in Chrome DevTools with a large group (50+ photos)
- [x] 3.2 Verify in the Network tab that images below the fold are not requested on initial load
- [x] 3.3 Scroll down and confirm images fetch only as they approach the viewport
