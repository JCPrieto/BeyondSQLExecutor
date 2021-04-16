package es.jklabs.utilidades;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.StorageClient;
import com.google.firebase.database.*;
import es.jklabs.gui.MainUI;
import es.jklabs.gui.utilidades.Growls;
import es.jklabs.json.firebase.Aplicacion;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;

public class UtilidadesFirebase {

    private static final String REFERENCE = "aplicaciones/BeyondSQLExecutor";

    private UtilidadesFirebase() {

    }

    public static boolean existeNuevaVersion() throws IOException, InterruptedException {
        instanciarFirebase();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference(REFERENCE);
        Aplicacion app = getAplicacion(ref);
        return diferenteVersion(app.getUltimaVersion());
    }

    private static boolean diferenteVersion(String serverVersion) {
        String[] sv = serverVersion.split("\\.");
        String[] av = Constantes.VERSION.split("\\.");
        return Integer.parseInt(sv[0]) > Integer.parseInt(av[0]) || Integer.parseInt(sv[0]) == Integer.parseInt(av[0]) && (Integer.parseInt(sv[1]) > Integer.parseInt(av[1]) || Integer.parseInt(sv[1]) == Integer.parseInt(av[1]) && Integer.parseInt(sv[2]) > Integer.parseInt(av[2]));
    }

    private static Aplicacion getAplicacion(DatabaseReference ref) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Aplicacion app = new Aplicacion();
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Aplicacion snap = snapshot.getValue(Aplicacion.class);
                app.setNumDescargas(snap.getNumDescargas());
                app.setUltimaVersion(snap.getUltimaVersion());
                latch.countDown();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Logger.info(error.getMessage());
                latch.countDown();
            }
        });
        latch.await();
        return app;
    }

    private static void instanciarFirebase() throws IOException {
        if (FirebaseApp.getApps().isEmpty()) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials
                            .fromStream(Objects
                                    .requireNonNull(UtilidadesFirebase.class.getClassLoader()
                                            .getResourceAsStream("json/curriculum-a2a80-firebase-adminsdk-17wyo-de15a29f7c.json"))))
                    .setStorageBucket(Constantes.STORAGE_BUCKET).setDatabaseUrl
                            ("https://curriculum-a2a80.firebaseio.com").build();
            FirebaseApp.initializeApp(options);
        }
    }

    public static void descargaNuevaVersion(MainUI mainUI) throws InterruptedException {
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int retorno = fc.showSaveDialog(mainUI);
        if (retorno == JFileChooser.APPROVE_OPTION) {
            File directorio = fc.getSelectedFile();
            try {
                instanciarFirebase();
                Bucket bucket = StorageClient.getInstance().bucket();
                Storage storage = bucket.getStorage();
                Aplicacion app = getAplicacion(FirebaseDatabase.getInstance().getReference(REFERENCE));
                if (app.getUltimaVersion() != null) {
                    Blob blob = storage.get(Constantes.STORAGE_BUCKET, getNombreApp(app), Storage
                            .BlobGetOption.fields(Storage.BlobField.SIZE));
                    blob.downloadTo(Paths.get(directorio.getPath() + System.getProperty("file.separator") + getNombreApp(app)));
                    actualizarNumDescargas();
                    Growls.mostrarInfo(null, "nueva.version.descargada");
                } else {
                    Logger.info("Error de lectura de la BBDD");
                }
            } catch (AccessDeniedException e) {
                Growls.mostrarError(null, "path.sin.permiso.escritura", e);
                descargaNuevaVersion(mainUI);
            } catch (IOException e) {
                Logger.error("descargar.nueva.version", e);
            }
        }
    }

    private static void actualizarNumDescargas() throws IOException, InterruptedException {
        instanciarFirebase();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference(REFERENCE);
        Aplicacion app = getAplicacion(ref);
        Map<String, Object> map = new HashMap<>();
        map.put("numDescargas", (app.getNumDescargas() + 1));
        ref.updateChildrenAsync(map);
    }

    private static String getNombreApp(Aplicacion app) {
        return Constantes.NOMBRE_APP + "-" + app.getUltimaVersion() + ".zip";
    }
}
