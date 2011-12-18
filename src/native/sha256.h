/*
 * Declarations of functions and data types used for SHA256 sum
 * library functions.
 * Copyright (C) 2005-2006, 2008-2011 Free Software Foundation, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

/*
 * http://git.savannah.gnu.org/gitweb/?p=gnulib.git;a=blob;f=lib/sha256.h;h=9f6bf14bf0c7f806f89edd969194bd148611039c;hb=d60f3b0c6b0f93a601acd1cfd3923f94ca05abb0
 */

/*
 * NOTICE The original file containd the function signatures for SHA224
 * as well. They have been removed from this implementation since they
 * are not used in our project.
 *
 * See the link above for the original file including the SHA224
 * implementation.
 */

#ifndef SHA256_H
#define SHA256_H 1

#include <stdio.h>
#include <stdint.h>

#ifdef __cplusplus
extern "C"
{
#endif

    /**
     * Structure to save state of computation between the single steps.
     */
    struct sha256_ctx
    {
        uint32_t state[8];

        uint32_t total[2];
        size_t buflen;
        uint32_t buffer[32];
    };

    /*
     * Initialize structure containing state of computation.
     */
    extern void sha256_init_ctx ( struct sha256_ctx *ctx );

    /**
     * Starting with the result of former calls of this function (or the
     * initialization function update the context for the next LEN bytes
     * starting at BUFFER.
     * It is necessary that LEN is a multiple of 64!!!
     */
    extern void sha256_process_block ( const void *buffer, size_t len,
                                           struct sha256_ctx *ctx );

    /**
     * Starting with the result of former calls of this function (or the
     * initialization function update the context for the next LEN bytes
     * starting at BUFFER.
     * It is NOT required that LEN is a multiple of 64.
     */
    extern void sha256_process_bytes ( const void *buffer, size_t len,
                                           struct sha256_ctx *ctx );

    /**
     * Process the remaining bytes in the buffer and put result from CTX
     * in first 32 bytes following RESBUF. The result is always in little
     * endian byte order, so that a byte-wise output yields to the wanted
     * ASCII representation of the message digest.
     */
    extern void *sha256_finish_ctx ( struct sha256_ctx *ctx, void *resbuf );


    /**
     * Put result from CTX in first 32 bytes following RESBUF. The result is
     * always in little endian byte order, so that a byte-wise output yields
     * to the wanted ASCII representation of the message digest.
     */
    extern void *sha256_read_ctx ( const struct sha256_ctx *ctx, void *resbuf );


    /**
     * Compute SHA256 message digest for bytes read from STREAM. The
     * resulting message digest number will be written into the 32 bytes
     * beginning at RESBLOCK.
     */
    extern int sha256_stream ( FILE *stream, void *resblock );

    /**
     * Compute SHA256 message digest for LEN bytes beginning at BUFFER. The
     * result is always in little endian byte order, so that a byte-wise
     * output yields to the wanted ASCII representation of the message
     * digest.
     */
    extern void *sha256_buffer ( const char *buffer, size_t len, void *resblock );

#ifdef __cplusplus
}
#endif

#endif

