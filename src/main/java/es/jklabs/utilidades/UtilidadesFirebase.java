package es.jklabs.utilidades;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.*;
import es.jklabs.json.firebase.Aplicacion;

import java.io.IOException;
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
                    .setCredentials(GoogleCredentials.fromStream(UtilidadesFirebase.class.getClassLoader()
                            .getResourceAsStream
                                    ("json/curriculum-a2a80-firebase-adminsdk-17wyo-de15a29f7c.json")))
                    .setStorageBucket(Constantes.STORAGE_BUCKET).setDatabaseUrl
                            ("https://curriculum-a2a80.firebaseio.com").build();
            FirebaseApp.initializeApp(options);
        }
    }
}
