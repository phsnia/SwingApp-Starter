package com.example.HMS.GUI.Components.DragDrop;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static javax.swing.SwingUtilities.invokeAndWait;
import static org.assertj.core.api.Assertions.assertThat;

class SwingFileDropZoneTest {

    @TempDir
    Path tempDir;

    @Test
    void acceptsOneFileWithItsNormalizedAbsolutePath() throws Exception {
        Path file = Files.writeString(tempDir.resolve("one.txt"), "content");

        SwingFileDropZone[] zone = new SwingFileDropZone[1];
        AtomicReference<List<Path>> accepted = new AtomicReference<>();
        List<String> rejected = new ArrayList<>();
        onEdt(() -> {
            zone[0] = new SwingFileDropZone();
            zone[0].onFilesDropped(accepted::set).onDropRejected(rejected::add);

            boolean imported = importFiles(zone[0], file.toFile());

            assertThat(imported).isTrue();
            assertThat(accepted.get()).containsExactly(file.toAbsolutePath().normalize());
            assertThat(rejected).isEmpty();
        });
        awaitSettled(zone[0]);
    }

    @Test
    void acceptsMultipleFilesInOneCallback() throws Exception {
        Path first = Files.writeString(tempDir.resolve("first.txt"), "1");
        Path second = Files.writeString(tempDir.resolve("second.txt"), "2");

        SwingFileDropZone[] zone = new SwingFileDropZone[1];
        AtomicReference<List<Path>> accepted = new AtomicReference<>();
        onEdt(() -> {
            zone[0] = new SwingFileDropZone();
            zone[0].onFilesDropped(accepted::set);

            importFiles(zone[0], first.toFile(), second.toFile());

            assertThat(accepted.get()).containsExactly(
                    first.toAbsolutePath().normalize(),
                    second.toAbsolutePath().normalize());
        });
        awaitSettled(zone[0]);
    }

    @Test
    void rejectsUnsupportedTransferFlavors() {
        onEdt(() -> {
            SwingFileDropZone zone = new SwingFileDropZone();
            List<String> rejected = new ArrayList<>();
            List<List<Path>> accepted = new ArrayList<>();
            zone.onFilesDropped(accepted::add).onDropRejected(rejected::add);

            boolean imported = handler(zone).importData(zone.peer(), new StringSelection("text"));

            assertThat(imported).isFalse();
            assertThat(accepted).isEmpty();
            assertThat(rejected).containsExactly("Only local files can be dropped here.");
        });
    }

    @Test
    void rejectsAnEmptyFileList() {
        onEdt(() -> {
            SwingFileDropZone zone = new SwingFileDropZone();
            List<String> rejected = new ArrayList<>();
            zone.onDropRejected(rejected::add);

            boolean imported = importFiles(zone);

            assertThat(imported).isFalse();
            assertThat(rejected).containsExactly("No files were dropped.");
        });
    }

    @Test
    void rejectsDirectories() {
        onEdt(() -> {
            SwingFileDropZone zone = new SwingFileDropZone();
            List<String> rejected = new ArrayList<>();
            zone.onDropRejected(rejected::add);

            boolean imported = importFiles(zone, tempDir.toFile());

            assertThat(imported).isFalse();
            assertThat(rejected).containsExactly("Folders are not supported. Drop files instead.");
        });
    }

    @Test
    void rejectsMissingFiles() {
        onEdt(() -> {
            SwingFileDropZone zone = new SwingFileDropZone();
            List<String> rejected = new ArrayList<>();
            zone.onDropRejected(rejected::add);

            boolean imported = importFiles(zone, tempDir.resolve("missing.txt").toFile());

            assertThat(imported).isFalse();
            assertThat(rejected).containsExactly("Only existing files can be dropped.");
        });
    }

    @Test
    void rejectsTheWholeDropWhenOneEntryIsInvalid() throws Exception {
        Path valid = Files.writeString(tempDir.resolve("valid.txt"), "ok");

        onEdt(() -> {
            SwingFileDropZone zone = new SwingFileDropZone();
            List<List<Path>> accepted = new ArrayList<>();
            List<String> rejected = new ArrayList<>();
            zone.onFilesDropped(accepted::add).onDropRejected(rejected::add);

            boolean imported = importFiles(zone, valid.toFile(), tempDir.toFile());

            assertThat(imported).isFalse();
            assertThat(accepted).isEmpty();
            assertThat(rejected).containsExactly("Folders are not supported. Drop files instead.");
        });
    }

    @Test
    void aDisabledZoneInvokesNeitherCallback() throws Exception {
        Path file = Files.writeString(tempDir.resolve("disabled.txt"), "content");

        onEdt(() -> {
            SwingFileDropZone zone = new SwingFileDropZone().setEnabled(false);
            List<List<Path>> accepted = new ArrayList<>();
            List<String> rejected = new ArrayList<>();
            zone.onFilesDropped(accepted::add).onDropRejected(rejected::add);

            boolean imported = importFiles(zone, file.toFile());

            assertThat(imported).isFalse();
            assertThat(zone.peer().isEnabled()).isFalse();
            assertThat(accepted).isEmpty();
            assertThat(rejected).isEmpty();
        });
    }

    @Test
    void promptChangesTheVisibleInstruction() {
        onEdt(() -> {
            SwingFileDropZone zone = new SwingFileDropZone();

            zone.setPrompt("Choose files");

            JLabel label = findLabel(zone.peer());
            assertThat(label).isNotNull();
            assertThat(label.getText()).isEqualTo("Choose files");
        });
    }

    @Test
    void callbacksAreDeliveredOnTheEdtEvenWhenImportRunsElsewhere() throws Exception {
        Path file = Files.writeString(tempDir.resolve("edt.txt"), "ok");
        AtomicBoolean onEdt = new AtomicBoolean(false);
        SwingFileDropZone[] zone = new SwingFileDropZone[1];
        onEdt(() -> zone[0] = new SwingFileDropZone()
                .onFilesDropped(paths -> onEdt.set(SwingUtilities.isEventDispatchThread())));

        boolean imported = importFiles(zone[0], file.toFile());
        assertThat(imported).isTrue();

        onEdt(() -> {
            // Drain the callback queued by the off-EDT import above.
        });
        assertThat(onEdt).isTrue();
        awaitSettled(zone[0]);
    }

    @Test
    void droppingAnImageShowsAProportionalPreview() throws Exception {
        Path text = Files.writeString(tempDir.resolve("note.txt"), "not an image");
        Path image = imageFile("wide.png", 100, 50);

        SwingFileDropZone[] zone = new SwingFileDropZone[1];
        AtomicReference<List<Path>> accepted = new AtomicReference<>();
        onEdt(() -> {
            zone[0] = new SwingFileDropZone();
            zone[0].onFilesDropped(accepted::set);
            assertThat(importFiles(zone[0], text.toFile(), image.toFile())).isTrue();
        });

        awaitPreview(zone[0], 100, 50);
        onEdt(() -> {
            assertThat(accepted.get()).containsExactly(
                    text.toAbsolutePath().normalize(),
                    image.toAbsolutePath().normalize());
            assertThat(zone[0].previewScaledSize(200, 200)).isEqualTo(new Dimension(200, 100));
            assertThat(zone[0].previewScaledSize(50, 200)).isEqualTo(new Dimension(50, 25));
        });
    }

    @Test
    void aNonImageDropClearsThePreviousPreview() throws Exception {
        Path image = imageFile("previous.png", 60, 30);
        Path text = Files.writeString(tempDir.resolve("plain.txt"), "plain");

        SwingFileDropZone[] zone = new SwingFileDropZone[1];
        onEdt(() -> zone[0] = new SwingFileDropZone());

        onEdt(() -> assertThat(importFiles(zone[0], image.toFile())).isTrue());
        awaitPreview(zone[0], 60, 30);

        onEdt(() -> assertThat(importFiles(zone[0], text.toFile())).isTrue());
        awaitSettled(zone[0]);
        onEdt(() -> assertThat(zone[0].previewImage()).isNull());
    }

    @Test
    void aCorruptImageIsAcceptedButNotPreviewed() throws Exception {
        Path corrupt = Files.write(tempDir.resolve("broken.png"), new byte[]{1, 2, 3, 4});

        SwingFileDropZone[] zone = new SwingFileDropZone[1];
        AtomicReference<List<Path>> accepted = new AtomicReference<>();
        onEdt(() -> {
            zone[0] = new SwingFileDropZone();
            zone[0].onFilesDropped(accepted::set);
            assertThat(importFiles(zone[0], corrupt.toFile())).isTrue();
        });

        awaitSettled(zone[0]);
        onEdt(() -> {
            assertThat(accepted.get()).containsExactly(corrupt.toAbsolutePath().normalize());
            assertThat(zone[0].previewImage()).isNull();
        });
    }

    @Test
    void aLaterDropReplacesThePreview() throws Exception {
        Path first = imageFile("first.png", 50, 20);
        Path second = imageFile("second.png", 20, 50);

        SwingFileDropZone[] zone = new SwingFileDropZone[1];
        onEdt(() -> zone[0] = new SwingFileDropZone());

        onEdt(() -> assertThat(importFiles(zone[0], first.toFile())).isTrue());
        awaitPreview(zone[0], 50, 20);

        onEdt(() -> assertThat(importFiles(zone[0], second.toFile())).isTrue());
        awaitPreview(zone[0], 20, 50);
    }

    @Test
    void aStalePreviewGenerationCannotReplaceANewerPreview() {
        BufferedImage oldImage = new BufferedImage(40, 20, BufferedImage.TYPE_INT_ARGB);
        BufferedImage newImage = new BufferedImage(20, 40, BufferedImage.TYPE_INT_ARGB);

        onEdt(() -> {
            SwingFileDropZone zone = new SwingFileDropZone();
            long oldGeneration = zone.beginPreviewLoad();
            long newGeneration = zone.beginPreviewLoad();

            zone.applyPreview(newGeneration, newImage);
            zone.applyPreview(oldGeneration, oldImage);

            assertThat(zone.previewImage()).isSameAs(newImage);
        });
    }

    @Test
    void aSlowEarlierDecodeCannotReplaceANewerPreview() throws Exception {
        Path slow = Files.writeString(tempDir.resolve("slow.png"), "slow");
        Path fast = Files.writeString(tempDir.resolve("fast.png"), "fast");
        BufferedImage slowImage = new BufferedImage(90, 30, BufferedImage.TYPE_INT_ARGB);
        BufferedImage fastImage = new BufferedImage(30, 60, BufferedImage.TYPE_INT_ARGB);
        CountDownLatch slowStarted = new CountDownLatch(1);
        CountDownLatch releaseSlow = new CountDownLatch(1);
        CountDownLatch slowReturned = new CountDownLatch(1);

        SwingFileDropZone[] zone = new SwingFileDropZone[1];
        onEdt(() -> zone[0] = new SwingFileDropZone(source -> {
            if (source.getFileName().toString().equals("slow.png")) {
                slowStarted.countDown();
                awaitIgnoringInterrupts(releaseSlow);
                slowReturned.countDown();
                return slowImage;
            }
            return fastImage;
        }));

        onEdt(() -> assertThat(importFiles(zone[0], slow.toFile())).isTrue());
        assertThat(slowStarted.await(5, TimeUnit.SECONDS)).isTrue();

        onEdt(() -> assertThat(importFiles(zone[0], fast.toFile())).isTrue());
        awaitPreview(zone[0], 30, 60);

        releaseSlow.countDown();
        assertThat(slowReturned.await(5, TimeUnit.SECONDS)).isTrue();
        onEdt(() -> {
            // Drain any queued completion callback from the slow worker.
        });
        onEdt(() -> assertThat(zone[0].previewImage()).isSameAs(fastImage));
    }

    @Test
    void imageDecodingRunsOffTheEdt() throws Exception {
        Path path = Files.writeString(tempDir.resolve("decoded.png"), "ignored");
        BufferedImage image = new BufferedImage(30, 15, BufferedImage.TYPE_INT_ARGB);
        AtomicBoolean decodedOnEdt = new AtomicBoolean(true);
        SwingFileDropZone[] zone = new SwingFileDropZone[1];

        onEdt(() -> zone[0] = new SwingFileDropZone(source -> {
            decodedOnEdt.set(SwingUtilities.isEventDispatchThread());
            return image;
        }));

        onEdt(() -> assertThat(importFiles(zone[0], path.toFile())).isTrue());
        awaitPreview(zone[0], 30, 15);
        assertThat(decodedOnEdt.get()).isFalse();
    }

    @Test
    void previewBoundsCoverTheScaledImageInsideTheTarget() throws Exception {
        Path image = imageFile("bounds.png", 100, 50);

        SwingFileDropZone[] zone = new SwingFileDropZone[1];
        onEdt(() -> {
            zone[0] = new SwingFileDropZone();
            zone[0].peer().setSize(400, 200);
            assertThat(zone[0].previewBounds().isEmpty()).isTrue();
        });

        onEdt(() -> assertThat(importFiles(zone[0], image.toFile())).isTrue());
        awaitPreview(zone[0], 100, 50);

        onEdt(() -> {
            Rectangle bounds = zone[0].previewBounds();
            Insets insets = zone[0].peer().getInsets();
            int width = zone[0].peer().getWidth() - insets.left - insets.right;
            int height = zone[0].peer().getHeight() - insets.top - insets.bottom;
            Dimension scaled = zone[0].previewScaledSize(width, height);

            assertThat(bounds.width).isEqualTo(scaled.width);
            assertThat(bounds.height).isEqualTo(scaled.height);
            assertThat(bounds.x).isEqualTo(insets.left + (width - scaled.width) / 2);
            assertThat(bounds.y).isEqualTo(insets.top + (height - scaled.height) / 2);
        });
    }

    @Test
    void previewChangesNotifyTheListenerOnTheEdt() throws Exception {
        Path image = imageFile("notify.png", 40, 20);

        SwingFileDropZone[] zone = new SwingFileDropZone[1];
        AtomicInteger changes = new AtomicInteger();
        AtomicBoolean onEdt = new AtomicBoolean(true);
        onEdt(() -> {
            zone[0] = new SwingFileDropZone();
            zone[0].onPreviewChanged(() -> {
                changes.incrementAndGet();
                onEdt.set(onEdt.get() && SwingUtilities.isEventDispatchThread());
            });
        });

        onEdt(() -> assertThat(importFiles(zone[0], image.toFile())).isTrue());
        awaitPreview(zone[0], 40, 20);
        assertThat(changes.get()).isGreaterThanOrEqualTo(2);

        onEdt(() -> zone[0].clearPreview());
        assertThat(changes.get()).isGreaterThanOrEqualTo(3);
        assertThat(onEdt).isTrue();
    }

    @Test
    void clearPreviewRemovesTheCurrentImage() throws Exception {
        Path image = imageFile("clear.png", 70, 35);

        SwingFileDropZone[] zone = new SwingFileDropZone[1];
        onEdt(() -> zone[0] = new SwingFileDropZone());

        onEdt(() -> assertThat(importFiles(zone[0], image.toFile())).isTrue());
        awaitPreview(zone[0], 70, 35);

        onEdt(() -> zone[0].clearPreview());
        onEdt(() -> assertThat(zone[0].previewImage()).isNull());
    }

    @Test
    void clearPreviewStopsAnInFlightDecodeFromRestoringTheImage() throws Exception {
        Path slow = Files.writeString(tempDir.resolve("slow-clear.png"), "slow");
        BufferedImage image = new BufferedImage(80, 20, BufferedImage.TYPE_INT_ARGB);
        CountDownLatch slowStarted = new CountDownLatch(1);
        CountDownLatch releaseSlow = new CountDownLatch(1);
        CountDownLatch slowReturned = new CountDownLatch(1);

        SwingFileDropZone[] zone = new SwingFileDropZone[1];
        onEdt(() -> zone[0] = new SwingFileDropZone(source -> {
            slowStarted.countDown();
            awaitIgnoringInterrupts(releaseSlow);
            slowReturned.countDown();
            return image;
        }));

        onEdt(() -> assertThat(importFiles(zone[0], slow.toFile())).isTrue());
        assertThat(slowStarted.await(5, TimeUnit.SECONDS)).isTrue();

        onEdt(() -> zone[0].clearPreview());
        releaseSlow.countDown();
        assertThat(slowReturned.await(5, TimeUnit.SECONDS)).isTrue();
        onEdt(() -> {
            // Drain any queued completion callback from the cancelled worker.
        });
        onEdt(() -> assertThat(zone[0].previewImage()).isNull());
    }

    @Test
    void clearPreviewFromAnotherThreadIsDispatchedToTheEdt() throws Exception {
        BufferedImage image = new BufferedImage(20, 10, BufferedImage.TYPE_INT_ARGB);
        SwingFileDropZone[] zone = new SwingFileDropZone[1];
        onEdt(() -> {
            zone[0] = new SwingFileDropZone();
            zone[0].applyPreview(zone[0].beginPreviewLoad(), image);
        });

        assertThat(zone[0].clearPreview()).isSameAs(zone[0]);
        onEdt(() -> {
            // Drain the queued clear before asserting.
        });
        onEdt(() -> assertThat(zone[0].previewImage()).isNull());
    }

    @Test
    void fluentMethodsReturnTheSameInstance() {
        onEdt(() -> {
            SwingFileDropZone zone = new SwingFileDropZone();

            assertThat(zone.onFilesDropped(paths -> {
            })).isSameAs(zone);
            assertThat(zone.onDropRejected(reason -> {
            })).isSameAs(zone);
            assertThat(zone.setPrompt("Drop")).isSameAs(zone);
            assertThat(zone.setEnabled(true)).isSameAs(zone);
            assertThat(zone.clearPreview()).isSameAs(zone);
            assertThat(zone.onPreviewChanged(() -> {
            })).isSameAs(zone);
        });
    }

    private static TransferHandler handler(FileDropZone<JPanel> zone) {
        return zone.peer().getTransferHandler();
    }

    private static boolean importFiles(FileDropZone<JPanel> zone, File... files) {
        return handler(zone).importData(zone.peer(), fileTransfer(files));
    }

    private static Transferable fileTransfer(File... files) {
        return new Transferable() {
            @Override
            public DataFlavor[] getTransferDataFlavors() {
                return new DataFlavor[]{DataFlavor.javaFileListFlavor};
            }

            @Override
            public boolean isDataFlavorSupported(DataFlavor flavor) {
                return DataFlavor.javaFileListFlavor.equals(flavor);
            }

            @Override
            public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
                if (!isDataFlavorSupported(flavor)) {
                    throw new UnsupportedFlavorException(flavor);
                }
                return Arrays.asList(files);
            }
        };
    }

    private Path imageFile(String name, int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Path path = tempDir.resolve(name);
        assertThat(ImageIO.write(image, "png", path.toFile())).isTrue();
        return path;
    }

    private static void awaitPreview(SwingFileDropZone zone, int width, int height) throws Exception {
        long deadline = System.currentTimeMillis() + 5000;
        int[] actual = {-1, -1};
        while (System.currentTimeMillis() < deadline) {
            onEdt(() -> {
                Image image = zone.previewImage();
                actual[0] = image == null ? -1 : image.getWidth(null);
                actual[1] = image == null ? -1 : image.getHeight(null);
            });
            if (actual[0] == width && actual[1] == height) {
                return;
            }
            Thread.sleep(10);
        }
        throw new AssertionError("Expected preview " + width + "x" + height
                + " but was " + actual[0] + "x" + actual[1]);
    }

    private static void awaitSettled(SwingFileDropZone zone) throws Exception {
        long deadline = System.currentTimeMillis() + 5000;
        boolean[] loading = {true};
        while (System.currentTimeMillis() < deadline) {
            onEdt(() -> loading[0] = zone.isPreviewLoading());
            if (!loading[0]) {
                onEdt(() -> {
                    // Let a queued done() callback run before the assertion.
                });
                return;
            }
            Thread.sleep(10);
        }
        throw new AssertionError("Preview loading did not settle");
    }

    private static JLabel findLabel(Component component) {
        if (component instanceof JLabel label) {
            return label;
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                JLabel found = findLabel(child);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static void awaitIgnoringInterrupts(CountDownLatch latch) {
        while (latch.getCount() > 0) {
            try {
                latch.await(10, TimeUnit.MILLISECONDS);
            } catch (InterruptedException ignored) {
                // Keep waiting to simulate an image decoder that cannot be cancelled.
            }
        }
    }

    private static void onEdt(Runnable action) {
        try {
            invokeAndWait(action);
        } catch (InvocationTargetException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof RuntimeException runtime) {
                throw runtime;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new RuntimeException(cause);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(ex);
        }
    }
}
