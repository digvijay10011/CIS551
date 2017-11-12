keytool -genkey -alias sharedsecret -keystore $1 -storetype JKS -keyalg EC -keysize 256 -sigalg SHA256withECDSA -dname "O=bank" -storepass $2 -keypass $2
