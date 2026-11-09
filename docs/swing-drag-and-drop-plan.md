# Swing Drag-and-Drop Component Plan

Phased plan for a reusable drag-and-drop component in
`com.example.HMS.GUI.Components`.

**Initial scope:** v0.1 accepts local files dropped onto a target and previews
an image from the dropped files. Text drops, reordering, and general
component-to-component transfers are explicitly later versions so the first API
stays small and testable.

This plan is separate from the main [Swing component plan](./swing-components-plan.md)
because drag-and-drop has its own interaction, transfer, and testing concerns.

---

## Goal and boundaries

Provide a small Swing component that screens can use to receive files from the
desktop without depending directly on Swing's `TransferHandler`.

The component:

- accepts one or more local regular files dropped onto its visible target;
- reports accepted files to a screen as `List<Path>`;
- displays a preview of the first supported image in each accepted drop;
- exposes a Swing peer for layout;
- reports rejected drops through an explicit callback;
- does not copy, move, upload, or otherwise process the files;
- does not initiate drags or support text, item reordering, or arbitrary
  `Transferable` values in v0.1.

Screens remain responsible for deciding what to do with accepted paths.

---

## Component dependency graph

```text
UI factory
   │
   ▼
FileDropZone interface
   │
   ▼
SwingFileDropZone
   │
   ├── JPanel peer
   ├── TransferHandler (file-list import only)
   └── ImageIO preview (display only)
```

The screen depends on `FileDropZone`; only the component implementation uses
Swing's transfer API directly.

---

## Version 0.1 — File drop target with image preview (simple)

**Goal:** A screen can receive a validated list of files by dropping them on a
visible target, and image drops provide a visual preview in that target.

### Component files and integration points

- `Components/DragDrop/FileDropZone.java` — public component interface
  (implemented).
- `Components/DragDrop/SwingFileDropZone.java` — Swing implementation; add
  image preview here.
- `Components/DragDrop/` tests — API and transfer handling tests; extend with
  image preview coverage.
- `Core/UI.java` — `fileDropZone()` factory method (implemented).
- `SwingDemo.java` — file-drop example; extend to show image previews.

### Proposed API

```java
public interface FileDropZone<P extends JComponent> {

    FileDropZone<P> onFilesDropped(Consumer<List<Path>> handler);

    FileDropZone<P> onDropRejected(Consumer<String> handler);

    FileDropZone<P> setPrompt(String prompt);

    FileDropZone<P> setEnabled(boolean enabled);

    P peer();
}
```

The implementation is `SwingFileDropZone implements FileDropZone<JPanel>`.
`UI.fileDropZone()` should return the interface, not the implementation, so
screens can depend on the component contract.

### API behavior

- `onFilesDropped` receives an immutable list of normalized absolute paths.
- A drop is accepted only when Swing offers the native file-list flavor and all
  entries are existing regular files. Unsupported flavors, empty lists,
  directories, or invalid entries are rejected as a whole; partial acceptance
  is not performed.
- For an accepted drop, the target displays the first file in that drop that
  can be decoded as an image by the installed Java `ImageIO` readers. A later
  accepted drop replaces the previous preview. Non-image files remain valid
  file drops and are still reported, but do not produce a preview.
- The preview is scaled proportionally to fit the target without changing the
  image's aspect ratio. If the drop contains no decodable image, show the
  prompt/empty-preview state instead.
- Image decoding runs off the EDT; updating the preview and invoking Swing
  callbacks happen on the EDT. If another drop arrives while an image is
  loading, only the most recent drop may update the preview.
- An image that cannot be decoded is not a rejected file drop: the valid paths
  are still reported, and the preview remains empty. File-list validation
  errors continue to use `onDropRejected`.
- `onDropRejected` receives a short, user-displayable reason when a drop is
  unsupported or invalid. A rejected drop does not invoke `onFilesDropped`.
- Disabled targets reject drops and do not invoke either successful-drop
  callback.
- `setPrompt` changes the target's visible instruction; it does not change
  accepted file types.
- `clearPreview` removes the current preview and cancels a decode that is still
  running. It does not change the reported paths or validate a new file.
- `previewBounds` and `onPreviewChanged` let a screen overlay its own controls on
  the drawn image without changing how drops are validated.
- Drop callbacks run on the Swing event dispatch thread. The component loads
  preview images in the background; screens must move their own additional file
  processing to a worker thread.
- The component does not retain dropped paths or perform file operations beyond
  reading a candidate image for its visual preview.
- Fluent methods return the same component instance.

### Tasks

- [x] Confirm the interface names, callback semantics, and rejection policy
      before implementation.
- [x] Implement the file-list import using `TransferHandler`; do not add a drag
      source or custom transfer flavor.
- [x] Render a small target panel with a prompt and enabled/disabled states,
      following the existing `Theme` and component conventions.
- [x] Add `UI.fileDropZone()` returning `FileDropZone<JPanel>`.
- [x] Add an example to `SwingDemo` showing accepted paths and rejected-drop
      feedback without reading file contents.
- [x] Document that callbacks run on the EDT and screen-owned file processing
      belongs to the consuming screen.
- [x] Add an image-preview area to the drop target without changing the public
      `FileDropZone` API.
- [x] Detect decodable images using `ImageIO` rather than a hard-coded file
      extension list; keep ordinary non-image files accepted.
- [x] Decode preview images off the EDT, scale them proportionally to the
      available target size, and apply them on the EDT.
- [x] Ensure a late image load from an earlier drop cannot replace the preview
      for a newer drop.
- [x] Extend the demo to show image previews and keep displaying accepted paths
      and rejection feedback.
- [x] Document that image preview is best-effort and does not change the
      accepted-file contract.
- [x] Add `clearPreview()` and a demo overlay X button so the preview can be
      discarded without changing reported paths.
- [x] Add `previewBounds()` and `onPreviewChanged()` so overlays follow the
      drawn image instead of the whole target.

### Verify

- A valid one-file and multi-file drop invokes `onFilesDropped` once with the
  expected paths.
- Dropping a supported image displays a proportional preview; dropping a
  non-image file still succeeds and leaves the empty-preview state visible.
- A mixed valid drop reports every path and previews the first decodable image.
- A corrupt or unsupported image does not crash or reject the valid file drop.
- A slower earlier image decode cannot overwrite the preview from a newer drop.
- `clearPreview()` removes the image, and a decode already in flight cannot
  restore it.
- The demo's circular X appears at the drawn image's top-right, clears the
  preview, and disappears.
- Unsupported transfer flavors, a directory, or an invalid entry invoke the
  rejection callback and never invoke the success callback.
- A disabled target invokes neither callback.
- Image detection, scaling, and stale-load handling are tested headlessly where
  possible; native desktop drag/drop and visual appearance are checked manually
  with `./grad.sh ui-demo`.
- `./gradlew test` passes.

**Status:** Implemented in `Components/DragDrop` as `FileDropZone` and
`SwingFileDropZone`; `UI.fileDropZone()` returns the interface and the demo has
a "File drop" page with image previews and a circular X overlay to clear them.
Callbacks are dispatched to the EDT.
Headless tests cover single and multi-file drops, empty lists, directories,
missing files, mixed invalid lists, unsupported flavors, disabled targets,
image preview, proportional scaling, non-image clears, corrupt images, later
drop replacement, stale-generation protection, off-EDT decoding, clearing a
preview during or after a load, drawn-preview bounds, and preview-change
notifications.

**Complexity:** 4/10.

---

## Version 0.2 — Text drops (deferred)

**Goal:** Support plain text without changing or weakening file-drop behavior.

### Tasks

- [ ] Agree whether text is a separate callback or a typed drop event before
      expanding the public API.
- [ ] Accept only the standard string flavor; keep unsupported flavors explicit.
- [ ] Add tests proving text and file drops are routed to the correct handler.
- [ ] Update the developer guide and demo.

### Verify

- Plain text is delivered exactly once.
- Existing file-drop tests and behavior remain unchanged.

**Complexity:** 4/10.

---

## Version 0.3 — Reorder items within a component (deferred)

**Goal:** Enable list-like components to reorder their own items through a
drag-and-drop gesture.

### Tasks

- [ ] Identify the first concrete component that needs reordering.
- [ ] Define how the source item, insertion position, and cancellation are
      represented without exposing internal Swing events.
- [ ] Keep reordering scoped to that component; do not introduce a general
      transfer framework prematurely.
- [ ] Add tests for reorder, same-position drop, and cancelled drag behavior.

### Verify

- Reordering updates the component's model once and preserves item ordering
      when a drag is cancelled or rejected.

**Complexity:** 6/10.

---

## Version 0.4 — General transfers (deferred)

**Goal:** Support typed transfers between components only after a concrete
cross-component use case exists.

### Tasks

- [ ] Define ownership, accepted operations, and payload lifetime.
- [ ] Decide whether a shared transfer contract is needed across components.
- [ ] Add explicit handling for copy, move, cancellation, and incompatible
      targets.
- [ ] Extend the API and tests only for demonstrated use cases.

### Verify

- Source and target agree on the operation and payload type.
- A failed or cancelled transfer leaves both components in a consistent state.

**Complexity:** 8/10.

---

## Conventions and definition of done

- Keep an interface and Swing implementation separate.
- Expose the Swing peer through `peer()`; screens should not install their own
  `TransferHandler` on the component.
- Keep file operations and business rules outside the component.
- Create and mutate the component on the EDT; offload expensive file processing.
- Validate each version with targeted tests, a demo, and updated documentation.
- Do not add a feature to an earlier version only because Swing's transfer API
  makes it technically possible; keep the public API driven by actual use cases.
