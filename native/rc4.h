/*
 * Code is taken from http://www.cypherspace.org/rsa/rc4.html and was adjusted
 * to the style guides and requirements within the CloudRAID project.
 */

#ifndef RC4_H
#define RC4_H 1

#include "defines.h"

#ifdef __cplusplus
extern "C"
{
#endif
    typedef struct rc4_key {
        unsigned char state[256];
        unsigned char x;
        unsigned char y;
    } rc4_key;

    LIBEXPORT void swap_byte(unsigned char *a, unsigned char *b);
    LIBEXPORT void prepare_key(const unsigned char *key_data_ptr, int key_data_len, rc4_key *key);
    LIBEXPORT void rc4(unsigned char *buffer_ptr, int buffer_len, rc4_key *key);

#ifdef __cplusplus
}
#endif

#endif

