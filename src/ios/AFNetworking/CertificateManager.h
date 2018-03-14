@interface CertificateManager: NSObject

+(NSArray*) getCertificates;
+(NSString*) getIv;
+(NSString*) getKey;
+(NSString*) decryptCipherTextWith:(NSString *)cipherText key:(NSString *)key iv:(NSString *)iv;

-(NSData*) decrypt:(NSData *)encryptedText key:(NSString *)key iv:(NSString *)iv;
-(NSString *) md5:(NSString *) input;

@end
