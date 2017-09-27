package william.miranda.comm.bluetooth;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.UUID;

/**
 * Servico para Ouvir um canal Bluetooth e processar as mensagens do Cliente
 */
public class BTServer extends Service {
    
    private static final String TAG = "BTServer";

    /**
     * Constantes das MSGs
     */
    public static final int MSG_LISTEN_SOCKET = 2;
    public static final int MSG_WAIT_MESSAGES = 3;
    public static final int MSG_EXIT = 4;
    
    public static final String UUID = "1234";

    /**
     * Instancia da Thread que ira escutar o canal
     */
    private ServerThread mServerThread;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Cria o Servico (Main Thread)
     */
    @Override
    public void onCreate() {
        super.onCreate();
        
        Log.d(TAG, "onCreate()");
        
        //inicia a Thread que ira fazer a comunicacao
        mServerThread = new ServerThread(this);
        mServerThread.start();
    }

    /**
     * Qualquer intent que chama o Servico cai aqui, entao devemos pegar os extras
     * e avisar a Thread que uma nova mensagem chegou
     * @param intent
     * @param flags
     * @param startId
     * @return
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    /**
     * Thread onde de fato as operacoes acontecem
     */
    public static class ServerThread extends Thread {

        private final Context mContext;
        private BluetoothServerSocket mServerSocket;
        private Handler mHandler;
        private DataInputStream mDataInputStream;
        private boolean isWaiting = true;

        public ServerThread(Context context) {
            mContext = context;
        }

        @Override
        public void run() {
            //prepara o Handler dessa nossa Thread
            Looper.prepare();

            mHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    switch (msg.what) {
                        case MSG_LISTEN_SOCKET:
                            Log.d(TAG, "Escutando o Socket");
                            listenSocket(UUID);
                            break;

                        case MSG_WAIT_MESSAGES:
                            while (isWaiting) {
                                Log.d(TAG, "Aguardando alguma mensagem");
                                String message = readMessage();
                                //notifica a Main Thread da mensagem que chegou
                            }
                            break;

                        case MSG_EXIT:
                            Looper looper = Looper.myLooper();
                            if (looper != null) {
                                looper.quit();
                                break;
                            }
                    }
                }
            };

            //nao deixa a thread morrer
            Looper.loop();
        }

        /**
         * Faz a conexao com o Servidor através do Socket
         */
        private void listenSocket(String sUuid) {

            try {
                //Cria o objeto UUID
                UUID uuid = UUID.nameUUIDFromBytes(sUuid.getBytes());

                //Obtem o socket
                mServerSocket = BluetoothAdapter.getDefaultAdapter().listenUsingRfcommWithServiceRecord("nome", uuid);

                //aguarda uma conexao (isso bloqueia a thread)
                BluetoothSocket socket = mServerSocket.accept();

                //se alguem se conectou, abre o Stream
                mDataInputStream = new DataInputStream(socket.getInputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public String readMessage() {
            if (mDataInputStream != null) {
                try {
                    return mDataInputStream.readUTF();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            return null;
        }

        public Handler getHandler() {
            return mHandler;
        }
    }
}
