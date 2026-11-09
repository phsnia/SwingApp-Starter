package com.example.HMS.GUI.Components.DragDrop;

import com.example.HMS.GUI.Components.Core.Theme;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.TransferHandler;
import javax.swing.border.Border;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/**
 * Default {@link FileDropZone} implementation: a panel with a prompt label, an
 * image-preview area, and a file-list-only {@link TransferHandler}.
 */
public class SwingFileDropZone implements FileDropZone<JPanel> {
    private static final String DEFAULT_PROMPT = "Drop files here";
    private static final String EMPTY_CARD = "empty";
    private static final String PREVIEW_CARD = "preview";

    private final JPanel peer = new JPanel(new BorderLayout());
    private final JLabel prompt = new JLabel(DEFAULT_PROMPT, SwingConstants.CENTER);
    private final PreviewPanel preview = new PreviewPanel();
    private final CardLayout previewLayout = new CardLayout();
    private final JPanel previewCards = new JPanel(previewLayout);
    private final ImageSource imageSource;

    private Consumer<List<Path>> filesDropped;
    private Consumer<String> dropRejected;
    private Runnable previewChanged;
    private boolean enabled = true;
    private long previewGeneration;
    private SwingWorker<Image, Void> previewWorker;

    public SwingFileDropZone() {
        this(SwingFileDropZone::readImage);
    }

    SwingFileDropZone(ImageSource imageSource) {
        this.imageSource = Objects.requireNonNull(imageSource, "imageSource");
        prompt.setFont(Theme.FONT);
        prompt.setForeground(Theme.MUTED_TEXT);

        previewCards.setOpaque(false);
        previewCards.add(prompt, EMPTY_CARD);
        previewCards.add(preview, PREVIEW_CARD);

        peer.setOpaque(true);
        peer.add(previewCards, BorderLayout.CENTER);
        peer.setTransferHandler(new FileTransferHandler());
        applyState();
    }

    @Override
    public SwingFileDropZone onFilesDropped(Consumer<List<Path>> handler) {
        filesDropped = Objects.requireNonNull(handler, "handler");
        return this;
    }

    @Override
    public SwingFileDropZone onDropRejected(Consumer<String> handler) {
        dropRejected = Objects.requireNonNull(handler, "handler");
        return this;
    }

    @Override
    public SwingFileDropZone setPrompt(String text) {
        prompt.setText(text == null ? "" : text);
        return this;
    }

    @Override
    public SwingFileDropZone setEnabled(boolean enabled) {
        this.enabled = enabled;
        applyState();
        return this;
    }

    @Override
    public SwingFileDropZone clearPreview() {
        if (SwingUtilities.isEventDispatchThread()) {
            clearPreviewNow();
        } else {
            SwingUtilities.invokeLater(this::clearPreviewNow);
        }
        return this;
    }

    /**
     * Registers a callback invoked on the EDT after the drawn preview changes,
     * including when it becomes empty. Screens can use it to reposition their
     * own overlays using {@link #previewBounds()}.
     */
    public SwingFileDropZone onPreviewChanged(Runnable handler) {
        previewChanged = Objects.requireNonNull(handler, "handler");
        return this;
    }

    @Override
    public JPanel peer() {
        return peer;
    }

    private void clearPreviewNow() {
        // Invalidate any worker first so a late done() cannot restore the image.
        previewGeneration++;
        SwingWorker<Image, Void> worker = previewWorker;
        previewWorker = null;
        if (worker != null) {
            worker.cancel(true);
        }
        showPreview(null);
    }

    private void applyState() {
        peer.setBackground(enabled ? Theme.SURFACE : Theme.BACKGROUND);
        prompt.setForeground(enabled ? Theme.MUTED_TEXT : Theme.BORDER);
        prompt.setEnabled(enabled);
        peer.setBorder(zoneBorder(enabled ? Theme.BORDER : Theme.BACKGROUND));
        peer.setEnabled(enabled);
    }

    private static Border zoneBorder(Color color) {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createDashedBorder(color, 8f, 4f, 1f, false),
                BorderFactory.createEmptyBorder(24, 24, 24, 24));
    }

    private void reject(String reason) {
        if (dropRejected == null) {
            return;
        }
        if (SwingUtilities.isEventDispatchThread()) {
            dropRejected.accept(reason);
        } else {
            SwingUtilities.invokeLater(() -> dropRejected.accept(reason));
        }
    }

    private void deliverFiles(List<Path> paths) {
        if (filesDropped == null) {
            return;
        }
        List<Path> accepted = List.copyOf(paths);
        if (SwingUtilities.isEventDispatchThread()) {
            filesDropped.accept(accepted);
        } else {
            SwingUtilities.invokeLater(() -> filesDropped.accept(accepted));
        }
    }

    private void startPreviewLoad(List<Path> paths) {
        if (!SwingUtilities.isEventDispatchThread()) {
            List<Path> copy = List.copyOf(paths);
            SwingUtilities.invokeLater(() -> startPreviewLoad(copy));
            return;
        }

        SwingWorker<Image, Void> previous = previewWorker;
        if (previous != null) {
            previous.cancel(true);
        }

        long generation = beginPreviewLoad();
        SwingWorker<Image, Void> worker = new SwingWorker<>() {
            @Override
            protected Image doInBackground() {
                for (Path path : paths) {
                    if (isCancelled()) {
                        return null;
                    }
                    try {
                        Image image = imageSource.read(path);
                        if (image != null) {
                            return image;
                        }
                    } catch (IOException | RuntimeException ignored) {
                        // Not a decodable image; try the next file in the drop.
                    }
                }
                return null;
            }

            @Override
            protected void done() {
                if (generation != previewGeneration) {
                    return;
                }
                previewWorker = null;
                if (isCancelled()) {
                    return;
                }
                try {
                    applyPreview(generation, get());
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    applyPreview(generation, null);
                } catch (ExecutionException | CancellationException ex) {
                    applyPreview(generation, null);
                }
            }
        };
        previewWorker = worker;
        worker.execute();
    }

    // Package-private seams used by the preview worker and headless tests.
    long beginPreviewLoad() {
        long generation = ++previewGeneration;
        showPreview(null);
        return generation;
    }

    void applyPreview(long generation, Image image) {
        if (generation == previewGeneration) {
            showPreview(image);
        }
    }

    private void showPreview(Image image) {
        preview.setImage(image);
        previewLayout.show(previewCards, image == null ? EMPTY_CARD : PREVIEW_CARD);
        previewCards.revalidate();
        previewCards.repaint();
        if (previewChanged != null) {
            previewChanged.run();
        }
    }

    Image previewImage() {
        return preview.image();
    }

    Dimension previewScaledSize(int width, int height) {
        return preview.scaledSize(width, height);
    }

    /**
     * Returns the bounds of the drawn preview in {@link #peer()} coordinates, or
     * an empty rectangle when no image is shown. Screens can use this to place
     * their own controls, such as a clear button, over the image. Call it on the
     * EDT.
     */
    public Rectangle previewBounds() {
        Insets insets = peer.getInsets();
        int width = peer.getWidth() - insets.left - insets.right;
        int height = peer.getHeight() - insets.top - insets.bottom;
        Dimension scaled = preview.scaledSize(width, height);
        if (scaled.width <= 0 || scaled.height <= 0) {
            return new Rectangle();
        }
        int x = insets.left + (width - scaled.width) / 2;
        int y = insets.top + (height - scaled.height) / 2;
        return new Rectangle(x, y, scaled.width, scaled.height);
    }

    boolean isPreviewLoading() {
        return previewWorker != null && !previewWorker.isDone();
    }

    private static Image readImage(Path path) throws IOException {
        return ImageIO.read(path.toFile());
    }

    /** Decoder seam; production uses {@link ImageIO}. */
    @FunctionalInterface
    interface ImageSource {
        Image read(Path path) throws IOException;
    }

    /**
     * Accepts every drop while enabled so unsupported flavors can be reported
     * through {@link #onDropRejected}; the actual validation happens in
     * {@link #importData(JComponent, Transferable)}.
     */
    private final class FileTransferHandler extends TransferHandler {

        @Override
        public boolean canImport(TransferSupport support) {
            return enabled && support.isDrop();
        }

        @Override
        public boolean importData(TransferSupport support) {
            if (!enabled || !support.isDrop()) {
                return false;
            }
            return support.getComponent() instanceof JComponent component
                    && importData(component, support.getTransferable());
        }

        @Override
        public boolean importData(JComponent component, Transferable transferable) {
            if (!enabled) {
                return false;
            }
            if (transferable == null) {
                reject("No data was dropped.");
                return false;
            }
            if (!transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                reject("Only local files can be dropped here.");
                return false;
            }

            List<File> files;
            try {
                Object data = transferable.getTransferData(DataFlavor.javaFileListFlavor);
                if (!(data instanceof List<?> raw)) {
                    reject("Only local files can be dropped here.");
                    return false;
                }
                files = new ArrayList<>(raw.size());
                for (Object entry : raw) {
                    if (!(entry instanceof File file)) {
                        reject("Only local files can be dropped here.");
                        return false;
                    }
                    files.add(file);
                }
            } catch (UnsupportedFlavorException | IOException ex) {
                reject("Could not read the dropped files.");
                return false;
            }

            if (files.isEmpty()) {
                reject("No files were dropped.");
                return false;
            }

            List<Path> paths = new ArrayList<>(files.size());
            for (File file : files) {
                Path path = file.toPath().toAbsolutePath().normalize();
                if (Files.isDirectory(path)) {
                    reject("Folders are not supported. Drop files instead.");
                    return false;
                }
                if (!Files.isRegularFile(path)) {
                    reject("Only existing files can be dropped.");
                    return false;
                }
                paths.add(path);
            }

            if (filesDropped != null) {
                deliverFiles(paths);
            }
            startPreviewLoad(paths);
            return true;
        }
    }

    /**
     * Draws the current image scaled to fit the available space without changing
     * its aspect ratio. The original image is kept, so resizing re-renders it.
     */
    private static final class PreviewPanel extends JComponent {
        private static final long serialVersionUID = 1L;
        private static final Dimension PREFERRED = new Dimension(320, 180);

        private Image image;

        void setImage(Image image) {
            this.image = image;
            repaint();
        }

        Image image() {
            return image;
        }

        Dimension scaledSize(int width, int height) {
            if (image == null || width <= 0 || height <= 0) {
                return new Dimension(0, 0);
            }
            int imageWidth = image.getWidth(null);
            int imageHeight = image.getHeight(null);
            if (imageWidth <= 0 || imageHeight <= 0) {
                return new Dimension(0, 0);
            }
            double scale = Math.min((double) width / imageWidth, (double) height / imageHeight);
            int scaledWidth = Math.max(1, (int) Math.round(imageWidth * scale));
            int scaledHeight = Math.max(1, (int) Math.round(imageHeight * scale));
            return new Dimension(scaledWidth, scaledHeight);
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(PREFERRED);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            if (image == null || getWidth() <= 0 || getHeight() <= 0) {
                return;
            }
            Dimension scaled = scaledSize(getWidth(), getHeight());
            if (scaled.width <= 0 || scaled.height <= 0) {
                return;
            }
            int x = (getWidth() - scaled.width) / 2;
            int y = (getHeight() - scaled.height) / 2;
            Graphics2D g2 = (Graphics2D) graphics.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2.setRenderingHint(RenderingHints.KEY_RENDERING,
                        RenderingHints.VALUE_RENDER_QUALITY);
                g2.drawImage(image, x, y, scaled.width, scaled.height, null);
            } finally {
                g2.dispose();
            }
        }
    }
}
