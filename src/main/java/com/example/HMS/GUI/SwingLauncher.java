package com.example.HMS.GUI;

import com.example.HMS.GUI.Screens.LoginScreen;
import com.example.HMS.GUI.Screens.MainScreen;
import com.example.HMS.GUI.Screens.Manga.MangaDataSource;
import com.example.HMS.GUI.Screens.Manga.MangaListScreen;
import com.example.HMS.GUI.Screens.Manga.ServiceMangaDataSource;
import com.example.HMS.HmsApplication;
import com.example.HMS.Manga.service.MangaService;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * Desktop entry point. Boots the Spring context in the same JVM (Option A from
 * the plan) and then hands the service to the Swing screens.
 */
public final class SwingLauncher {

    private SwingLauncher() {
    }

    public static void main(String[] args) {
        Theme.install();

        ConfigurableApplicationContext context;
        try {
            context = new SpringApplicationBuilder(HmsApplication.class)
                    .web(WebApplicationType.NONE)
                    .headless(false)
                    .run(args);
        } catch (Exception ex) {
            ex.printStackTrace();
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null,
                    "Could not start the application context:\n" + rootMessage(ex)
                            + "\n\nCheck your database settings (.env) and try again.",
                    "HMS", JOptionPane.ERROR_MESSAGE));
            return;
        }

        MangaService service = context.getBean(MangaService.class);
        MangaDataSource dataSource = new ServiceMangaDataSource(service);

        Runtime.getRuntime().addShutdownHook(new Thread(context::close));

        SwingUtilities.invokeLater(() -> {
            UI ui = new UI();
            SwingApp app = ui.app("HMS");
            LoginScreen login = new LoginScreen(ui);
            login.onSubmit(credentials -> showMain(app, ui, dataSource));
            app.setContent(login);
            app.setStatus("Not signed in");
            app.show();
        });
    }

    private static void showMain(SwingApp app, UI ui, MangaDataSource dataSource) {
        MainScreen main = new MainScreen(ui);
        main.add("Manga", new MangaListScreen(ui, app.frame(), dataSource));
        app.title("HMS - Manga Manager");
        app.setContent(main);
        app.setStatus("Signed in");
    }

    private static String rootMessage(Throwable ex) {
        Throwable current = ex;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }
}
