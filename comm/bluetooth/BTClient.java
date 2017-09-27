package william.miranda.comm.bluetooth;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.UUID;

/**
 * Servico para Conectar a um Servidor Bluetooth e
 * enviar mensagens pelo Socket.
 * O Servico roda na Main Thread, mas as operacoes de Rede devem rodar em outra Thread.
 * Usaremos um Handler para trocar dados.
 */
public class BTClient extends Service {

    private static final String TAG = "BTClient";

    /**
     * Constantes de MSGs aceitas pela Thread
     */
    public static final int MSG_OPEN_SOCKET = 2;
    public static final int MSG_SEND_MESSAGE = 3;
    public static final int MSG_EXIT = 4;
    
    public static final String SERVER_NAME = "BT_SERVER_NAME";
    public static final String UUID = "1234";

    /**
     * Device para quem iremos enviar os comandos
     */
    private BluetoothDevice mServerDevice;
    
    /**
     * Instancia da Thread que envia os comandos
     */
    private ClientThread mClientThread;

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
        
        //obtem o device alvo
        mServerDevice = getServerDevice();

        //inicia a Thread que ira fazer a comunicacao
        mClientThread = new ClientThread(this, mServerDevice);
        mClientThread.start();
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
        Log.d(TAG, "onStartCommand()");

        while (mClientThread.getHandler() == null) {

        }

        //nao ha como enviar mensagens
        if (mClientThread.getHandler() == null) {
            return START_STICKY;
        }

        //nao veio o que enviar
        if (intent == null) {
            return START_STICKY;
        }

        //se chegou aki, ha oq passar

        //obtemos o codigo da tarefa
        int code = intent.getIntExtra("code", -1);

        //codigo invalido
        if (code == -1) {
            return START_STICKY;
        }

        //define o codigo na mensagem
        Message message = Message.obtain();
        message.what = code;

        //adiciona extras se necessario
        if (code == MSG_SEND_MESSAGE) {
            //adiciona o Extra da mensagem
            Bundle bundle = new Bundle();
            bundle.putString("msg", intent.getStringExtra("msg"));
            message.setData(bundle);
        }

        //envia a mensagem para o Handler da Thread
        mClientThread.getHandler().sendMessage(message);

        return START_STICKY;
    }

    /**
     * Obtem o Device que roda o servidor baseado no nome
     * @return
     */
    private BluetoothDevice getServerDevice(String serverName) {
        
        if (serverName == null) {
            return null;
        }

        //Itera os Devices pareados
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        for (BluetoothDevice device : adapter.getBondedDevices()) {

            //Se achou o servidor, armazena
            if (serverName.equals(device.getName())) {
                return device;
            }
        }

        return null;
    }

    /**
     * Thread onde de fato as operacoes acontecem
     */
    public static class ClientThread extends Thread {

        private final Context mContext;
        private final BluetoothDevice mServerDevice;
        private Handler mHandler;
        private DataOutputStream mDataOutputStream;

        public ClientThread(Context context, BluetoothDevice serverDevice) {
            mContext = context;
            mServerDevice = serverDevice;
        }

        @Override
        public void run() {
            //prepara o Handler dessa nossa Thread
            Looper.prepare();

            mHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    switch (msg.what) {
                        case MSG_OPEN_SOCKET:
                            Log.d(TAG, "Abrindo o Socket");
                            openSocket(UUID);
                            break;

                        case MSG_SEND_MESSAGE:
                            Log.d(TAG, "Handle da mensagem");
                            String string = msg.getData().getString("msg");
                            sendBTMessage(string);
                            break;

                        case MSG_EXIT:
                            Log.d(TAG, "Finalizando a Thread");
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
        private void openSocket(String sUuid) {

            try {
                //Cria o objeto UUID
                UUID uuid = UUID.nameUUIDFromBytes(sUuid.getBytes());

                //Cria o Socket
                BluetoothSocket mSocket = mServerDevice
                        .createRfcommSocketToServiceRecord(uuid);

                //Faz a conexao
                mSocket.connect();

                //Obtem o Output Stream
                mDataOutputStream = new DataOutputStream(mSocket.getOutputStream());
            } catch (NullPointerException|IOException e) {
                e.printStackTrace();
            }
        }

        /**
         * Envia uma mensagem pelo Socket que foi aberto, utilizando o Stream
         * @param message
         */
        public void sendBTMessage(String message) {
            Log.d(TAG, "Enviando MSG");
            if (mDataOutputStream != null) {
                try {
                    mDataOutputStream.writeUTF(message);
                    Log.d(TAG, "MSG Enviada");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public Handler getHandler() {
            return mHandler;
        }
    }
}
