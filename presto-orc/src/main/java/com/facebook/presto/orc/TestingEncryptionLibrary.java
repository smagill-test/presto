/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.facebook.presto.orc;

import io.airlift.slice.Slice;
import io.airlift.slice.Slices;

import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.Base64.Decoder;

import static com.google.common.base.Verify.verify;

public class TestingEncryptionLibrary
        implements EncryptionLibrary
{
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder();
    private static final Decoder DECODER = Base64.getUrlDecoder();

    @Override
    public Slice encryptKey(Slice keyMetadata, byte[] input, int offset, int length)
    {
        return encrypt(keyMetadata, input, offset, length);
    }

    @Override
    public Slice encryptData(Slice keyMetadata, byte[] input, int offset, int length)
    {
        return encrypt(keyMetadata, input, offset, length);
    }

    @Override
    public Slice decryptKey(Slice keyMetadata, byte[] input, int offset, int length)
    {
        return decrypt(keyMetadata, input, offset, length);
    }

    @Override
    public Slice decryptData(Slice keyMetadata, byte[] input, int offset, int length)
    {
        return decrypt(keyMetadata, input, offset, length);
    }

    @Override
    public int getMaxEncryptedLength(Slice keyMetadata, int unencryptedLength)
    {
        return keyMetadata.length() + 4 * (unencryptedLength + 2) / 3;
    }

    private Slice encrypt(Slice keyMetadata, byte[] input, int offset, int length)
    {
        ByteBuffer inputBuffer = ByteBuffer.wrap(input, offset, length);
        ByteBuffer encoded = ENCODER.encode(inputBuffer);
        ByteBuffer output = ByteBuffer.allocate(keyMetadata.length() + encoded.remaining());
        output.put(keyMetadata.toByteBuffer());
        output.put(encoded);
        output.flip();
        return Slices.wrappedBuffer(output);
    }

    private Slice decrypt(Slice keyMetadata, byte[] input, int offset, int length)
    {
        ByteBuffer inputBuffer = ByteBuffer.wrap(input, offset, length);

        byte[] key = new byte[keyMetadata.length()];
        inputBuffer.get(key);
        verify(keyMetadata.equals(Slices.wrappedBuffer(key)), "keys do not match");

        ByteBuffer encoded = ByteBuffer.allocate(inputBuffer.remaining());
        encoded.put(inputBuffer);
        encoded.flip();
        ByteBuffer decodedByteBuffer = DECODER.decode(encoded);
        return Slices.wrappedBuffer(decodedByteBuffer);
    }
}
