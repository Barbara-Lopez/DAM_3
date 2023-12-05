import modelos.Client;
import modelos.Mensaje;
import modelos.Usuario;

import javax.crypto.Cipher;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.security.*;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Cliente {
    private static PublicKey clavepubServer;
    private static SSLSocket cliente;
    private static PrivateKey clavepriv;
    private static ObjectInputStream objetoEntrada;
    private static ObjectOutputStream objetoSalida;
    private static PublicKey clavepub;
    private static  Cipher rsaCipher;
    private static Scanner lectura = new Scanner(System.in);
    private static DataOutputStream textoSalida;
    private static DataInputStream textoEntrada;

    public static void main(String[] args) {
        System.setProperty("javax.net.ssl.trustStore", "src/files/UsuarioAlmacenSSL");
        System.setProperty("javax.net.ssl.trustStorePassword", "890123");
        try{
            //Crear el Socket Seguro
            String host="localhost";
            int puerto=6000;
            System.out.println("Programa cliente iniciado..");
            SSLSocketFactory sfact=(SSLSocketFactory)SSLSocketFactory.getDefault();
            cliente=(SSLSocket) sfact.createSocket(host,puerto);

            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            //SE CREA EL PAR DE CLAVES PRIVADA Y PÚBLICA
            KeyPair par = keyGen.generateKeyPair();
            clavepriv = par.getPrivate();
            clavepub = par.getPublic();
            rsaCipher = Cipher.getInstance("RSA");
            enviarRecibirClavePub();

            int op = 0;
            do {
                try {
                    System.out.println("Elija lo que quiere hacer: \n 1.Iniciar sesión \n 2.Crear cuenta \n 3.Salir");
                    op = Integer.parseInt(lectura.next());
                    switch(op){
                        case 1: {
                            // ENviar al servidor que opcion ha elegido
                            DataOutputStream op1= new DataOutputStream(cliente.getOutputStream());
                            op1.writeInt(1);
                            // función para iniciar sesion
                            inicioSesion();
                            // flujo de entrada que se utiliza para recoger si el usuario y contraseña es correcto
                            textoEntrada =new DataInputStream(cliente.getInputStream());
                            Boolean inicioOk= textoEntrada.readBoolean();
                            if(inicioOk){
                                textoEntrada =new DataInputStream(cliente.getInputStream());
                                String textoFirmar= textoEntrada.readUTF();
                                while(true){
                                    System.out.println("El siguiente texto hay que firmarlo, escriba 'si' en el caso de querer firmarlo:\n" +
                                            textoFirmar);
                                    String firma = lectura.next();
                                    if(firma.equalsIgnoreCase("si")){
                                        Signature rsaSignature = Signature.getInstance("SHA256withRSA");
                                        rsaSignature.initSign(clavepriv);
                                        rsaSignature.update(textoFirmar.getBytes());
                                        byte[] firmaHecha = rsaSignature.sign();
                                        ObjectOutputStream firmaDigital= new ObjectOutputStream(cliente.getOutputStream());
                                        firmaDigital.writeObject(new Mensaje(textoFirmar,firmaHecha));
                                        break;
                                    }else{
                                        ObjectOutputStream firmaDigital= new ObjectOutputStream(cliente.getOutputStream());
                                        byte[] firmaHecha = "No se ha firmado".getBytes();
                                        firmaDigital.writeObject(new Mensaje(textoFirmar,firmaHecha));
                                        break;
                                    }
                                }
                                textoEntrada =new DataInputStream(cliente.getInputStream());
                                Boolean firma= textoEntrada.readBoolean();
                                if(firma){
                                    System.out.println("Firmado correctamente");
                                }else{
                                    System.out.println("Fallo en la firma, no puede hacer ninguna operación");
                                }
                            }else{
                                System.out.println("Usuario o contraseña incorrecto");
                            }
                            break;
                        }
                        case 2: {
                            // ENviar al servidor que opcion ha elegido
                            DataOutputStream  op1= new DataOutputStream(cliente.getOutputStream());
                            op1.writeInt(2);
                            // función para crear una nueva cuenta
                            crearCuenta();
                            // flujo de entrada que se utiliza para recoger si la nueva cuenta se ha ceado correctamente
                            textoEntrada =new DataInputStream(cliente.getInputStream());
                            Boolean inicioOk= textoEntrada.readBoolean();
                            String testo=textoEntrada.readUTF();
                            if(inicioOk){
                                System.out.println(testo);
                            }else{
                                System.out.println(testo);
                            }
                            break;
                        }
                        case 3: {
                            // ENviar al servidor que opcion ha elegido
                            DataOutputStream  op1= new DataOutputStream(cliente.getOutputStream());
                            op1.writeInt(3);
                            break;
                        }
                        default:
                            System.out.println("Escriba un numero del 1 al 3");
                    }

                } catch (NumberFormatException ex){
                    System.out.println("Tienes que escribir un numero");
                } catch (Exception e){
                    System.out.println(e.getMessage());
                }
            } while (op!=3);
            cliente.close();

        }catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    public static void enviarRecibirClavePub() throws IOException, ClassNotFoundException {
        objetoEntrada=new ObjectInputStream(cliente.getInputStream());
        clavepubServer= (PublicKey) objetoEntrada.readObject();
        //System.out.println("Clave server:\n"+clavepubServer);
        objetoSalida = new ObjectOutputStream(cliente.getOutputStream());
        objetoSalida.writeObject(clavepub);
        //System.out.println("Clave cliente:\n"+clavepub);
    }
    public static void inicioSesion() throws IOException, NoSuchAlgorithmException {
        String user;
        while (true) {
            try{
                System.out.println("Escriba el usuario: ");
                user = lectura.next();
                break;
            }catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        String contrasena;
        while (true) {
            try{
                System.out.println("Escriba la contraseña: ");
                contrasena = lectura.next();
                break;
            }catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        MessageDigest md = MessageDigest.getInstance("SHA");
        md.update(contrasena.getBytes());
        byte resumen[]=md.digest();

        Usuario u=new Usuario(user,resumen);
        objetoSalida = new ObjectOutputStream(cliente.getOutputStream());
        objetoSalida.writeObject(u);
    }
    public static void crearCuenta() throws NoSuchAlgorithmException, IOException {
        String nombre;
        while (true) {
            try{
                System.out.println("Escriba el nombre: ");
                nombre = lectura.next();
                if(nombre.isBlank()){
                    throw new Exception("El nombre no puede estar vacio");
                }
                break;
            }catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
        String apellido;
        while (true) {
            try{
                System.out.println("Escriba el apellido: ");
                apellido = lectura.next();
                if(apellido.isBlank()){
                    throw new Exception("El apellido no puede estar vacio");
                }
                break;
            }catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        Integer edad;
        while (true) {
            try{
                System.out.println("Escriba la edad: ");
                edad = Integer.parseInt(lectura.next());
                break;
            }catch (NumberFormatException e) {
                System.out.println("Tiene que escribir un numero que corresponda contu edad");
            }catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
        String email;
        while (true) {
            try{
                System.out.println("Escriba el email: ");
                email = lectura.next();
                Pattern p = Pattern.compile("^(?=.{1,64}@)[A-Za-z0-9_-]+(\\.[A-Za-z0-9_-]+)*@"
                        + "[^-][A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(\\.[A-Za-z]{2,})$");
                Matcher m = p.matcher(email);
                if(!m.find()){
                    throw new Exception("El email escrito no es correcto tiene que ser ' '@' '.'' o ' '.' '@' '.' ', vuelva ha escribirlo");
                }
                break;
            }catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }

        String user;
        while (true) {
            try{
                System.out.println("Escriba el usuario: ");
                user = lectura.next();
                if(user.isBlank()){
                    throw new Exception("El nombre del usuario no puede estar vacio");
                }
                break;
            }catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
        String contrasena;
        while (true) {
            try{
                System.out.println("Escriba la contraseña: ");
                contrasena = lectura.next();
                if(contrasena.isBlank()){
                    throw new Exception("La contraseña no puede estar vacia");
                }
                break;
            }catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
        MessageDigest md = MessageDigest.getInstance("SHA");
        md.update(contrasena.getBytes());
        byte resumen[]=md.digest();

        Usuario u=new Usuario(user,resumen);
        Client c=new Client(nombre,apellido,edad,email,u);
        objetoSalida = new ObjectOutputStream(cliente.getOutputStream());
        objetoSalida.writeObject(c);
    }
}
