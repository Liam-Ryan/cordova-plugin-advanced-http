#import "CertificateManager.h"

#import <CommonCrypto/CommonCryptor.h>
#import <CommonCrypto/CommonDigest.h>

@implementation CertificateManager

+(NSString*) getIv {
  NSString *iv = @"iv";
  return iv;
}

+(NSString*) getKey {
  NSString *key = @"key";
  return key;
}

+(NSArray*) getCertificates {
    NSArray *array = @[ certificates ];
    return array;
}

+(NSString *) decryptCipherTextWith:(NSString *)ciperText key:(NSString *)key iv:(NSString *)iv {
    return [[NSString alloc] initWithData:[[CertificateManager alloc] decrypt:[[NSData alloc] initWithBase64EncodedString:ciperText options:NSDataBase64DecodingIgnoreUnknownCharacters] key:key iv:iv] encoding:NSUTF8StringEncoding];
}

-(NSData *) decrypt:(NSData *)encryptedText key:(NSString *)key iv:(NSString *)iv {
    char keyPointer[kCCKeySizeAES256+2],
    ivPointer[kCCBlockSizeAES128+2];
    BOOL patchNeeded;

    patchNeeded = ([key length] > kCCKeySizeAES256+1);
    if(patchNeeded)
    {
        NSLog(@"Key length is longer %lu", (unsigned long)[[self md5:key] length]);
        key = [key substringToIndex:kCCKeySizeAES256];
    }

    [key getCString:keyPointer maxLength:sizeof(keyPointer) encoding:NSUTF8StringEncoding];
    [iv getCString:ivPointer maxLength:sizeof(ivPointer) encoding:NSUTF8StringEncoding];

    if (patchNeeded) {
        keyPointer[0] = '\0';
    }

    NSUInteger dataLength = [encryptedText length];

    size_t buffSize = dataLength + kCCBlockSizeAES128;

    void *buff = malloc(buffSize);

    size_t numBytesEncrypted = 0;

    CCCryptorStatus status = CCCrypt(kCCDecrypt,
                                     kCCAlgorithmAES128,
                                     kCCOptionPKCS7Padding,
                                     keyPointer, kCCKeySizeAES256,
                                     ivPointer,
                                     [encryptedText bytes], [encryptedText length],
                                     buff, buffSize,
                                     &numBytesEncrypted);
    if (status == kCCSuccess) {
        return [NSData dataWithBytesNoCopy:buff length:numBytesEncrypted];
    }

    free(buff);
    return nil;
}

-(NSString *) md5:(NSString *) input {
    const char *cStr = [input UTF8String];
    unsigned char digest[16];
    CC_MD5( cStr, (uint32_t)strlen(cStr), digest );

    NSMutableString *output = [NSMutableString stringWithCapacity:CC_MD5_DIGEST_LENGTH * 2];

    for(int i = 0; i < CC_MD5_DIGEST_LENGTH; i++)
        [output appendFormat:@"%02x", digest[i]];

    return  output;
}

@end

