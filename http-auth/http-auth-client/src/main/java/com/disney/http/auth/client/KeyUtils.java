/*******************************************************************************
 * © 2018 Disney | ABC Television Group
 *
 * Licensed under the Apache License, Version 2.0 (the "Apache License")
 * with the following modification; you may not use this file except in
 * compliance with the Apache License and the following modification to it:
 * Section 6. Trademarks. is deleted and replaced with:
 *
 * 6. Trademarks. This License does not grant permission to use the trade
 *     names, trademarks, service marks, or product names of the Licensor
 *     and its affiliates, except as required to comply with Section 4(c) of
 *     the License and to reproduce the content of the NOTICE file.
 *
 * You may obtain a copy of the Apache License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License with the above modification is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the Apache License for the specific
 * language governing permissions and limitations under the Apache License.
 *******************************************************************************/
package com.disney.http.auth.client;

import java.io.FileOutputStream;
import java.io.IOException;
import java.security.*;
import java.util.Base64;

/**
 * General utility functions to help with the generation of RSA keys, and reading/writing of keys from files.
 *
 * @author Rachel Kobayashi
 */
public class KeyUtils {
    public static String findKey(String data) throws IOException, UnrecoverableKeyException {
        String[] fileParts = data.split("-----");
        if(fileParts.length > 1){
            // need to find location of key
            int i;
            for(i=0; i<fileParts.length; i++){
                if(fileParts[i].contains("KEY") && i+1<fileParts.length){
                    break;
                }
            }
            String keyContent = fileParts[i+1].trim();
            String[] lines = keyContent.split("\n");
            String firstLine = lines[0];
            if(firstLine.contains("ENCRYPTED")){
                throw new UnrecoverableKeyException("Key is encrypted, could not import");
            } else {
                return keyContent.trim();
            }
        } else {
            return data;
        }
    }

    /**
     * Generate a RSA KeyPair object to be used for signing.
     *
     * @param keySize   the bit size of the key to generate. Options are 1024 or 2048.
     * @return          the generated KeyPair.
     * @throws NoSuchAlgorithmException
     */
    public static KeyPair generateKeyPair(int keySize) throws NoSuchAlgorithmException{
        if(keySize != 1024) {
            keySize = 2048;
        }
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(keySize);
        return keyGen.generateKeyPair();
    }

    /**
     * Generates a RSA KeyPair object to be used for signing.
     *
     * <p>
     * This method sets the bit size to 2048.
     * @return  the generated KeyPair.
     * @throws NoSuchAlgorithmException
     */
    public static KeyPair generateKeyPair() throws NoSuchAlgorithmException{
        return generateKeyPair(2048);
    }

    /**
     * Constructs a new KeyStore from the provided Key, under the provided alias.
     *
     * @param alias     string to label the location of the key.
     * @param key       key object to add to the KeyStore, will through certificate error if instance of PrivateKey
     * @param password  the password to use for the given alias.
     * @return          the KeyStore object.
     * @throws Exception
     */
    public static KeyStore makeKeyStore(String alias, Key key, String password) throws Exception{
        KeyStore nks = KeyStore.getInstance("JCEKS");
        nks.load(null);
        nks.setKeyEntry(alias, key, password.toCharArray(), null);
        return nks;
    }

    /**
     * Write the given key into the given file, base64 encoded with 64 characters per line.
     * <p>
     * With the correct prefix and suffix this will format the key into PCKS8 format (non-encrypted)
     *
     * @param key       the key to encode
     * @param fileName  the absolute path of the file to save the key into. If the file does not exist, it will be created.
     * @param prefix    the string to write before the encoded key bytes (will not be encoded)
     * @param suffix    the string to write after the encoded key bytes (will not be encoded)
     * @throws IOException
     */
    public static void writeKeyToFile(Key key, String fileName, String prefix, String suffix) throws IOException{
        byte[] keyBytes = key.getEncoded();
        String base64Key = Base64.getEncoder().encodeToString(keyBytes);

        FileOutputStream keyFOS = new FileOutputStream(fileName);
        if(prefix != null) {
            keyFOS.write(prefix.getBytes("UTF-8"));
        }
        for(int i=0; i<=base64Key.length()/64; i++){
            if((i+1)*64 > base64Key.length()){
                keyFOS.write(base64Key.substring(i*64, base64Key.length()).getBytes());
            } else {
                keyFOS.write(base64Key.substring(i * 64, (i + 1) * 64).getBytes());
                keyFOS.write("\n".getBytes());
            }
        }
        if(suffix != null) {
            keyFOS.write(suffix.getBytes("UTF-8"));
        }
        keyFOS.close();

    }

    /**
     * Write the given key into the given file, base64 encoded with 64 characters per line.
     *
     * @param key       the key to encode
     * @param fileName  the absolute path of the file to save the key into. If the file does not exist, it will be created.
     * @throws IOException
     */
    public static void writeKeyToFile(Key key, String fileName) throws IOException{
        writeKeyToFile(key, fileName, null, null);
    }

    /**
     * Writes a KeyStore containing the PublicKey under the provided alias, into a file.
     *
     * @param publicKey the public key to add to the KeyStore.
     * @param fileName  the absolute path of the file to save the key into. If the file does not exist, it will be created.
     * @param alias     the alias where the PublicKey should be put in the KeyStore
     * @param password  the password to use for the KeyStore file.
     * @throws Exception
     */
    public static void writePublicKeyStoreToFile(PublicKey publicKey, String fileName, String alias, String password) throws Exception {
        KeyStore ks = makeKeyStore(alias, publicKey, password);
        FileOutputStream keyFOS = new FileOutputStream(fileName);
        ks.store(keyFOS, password.toCharArray());
        keyFOS.close();
    }
}
