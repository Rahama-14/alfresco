/*
 * Copyright (C) 2005 Alfresco, Inc.
 *
 * Licensed under the Mozilla Public License version 1.1 
 * with a permitted attribution clause. You may obtain a
 * copy of the License at
 *
 *   http://www.alfresco.org/legal/license.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
package org.alfresco.repo.security.authentication;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Security;

import net.sf.acegisecurity.providers.encoding.BaseDigestPasswordEncoder;
import net.sf.acegisecurity.providers.encoding.PasswordEncoder;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;

import cryptix.jce.provider.CryptixCrypto;

/**
 * <p>
 * MD4 implementation of PasswordEncoder.
 * </p>
 * 
 * <p>
 * If a <code>null</code> password is presented, it will be treated as an
 * empty <code>String</code> ("") password.
 * </p>
 * 
 * <P>
 * As MD4 is a one-way hash, the salt can contain any characters.
 * </p>
 */
public class MD4PasswordEncoder extends BaseDigestPasswordEncoder implements PasswordEncoder
{

    static
    {
        try
        {
            MessageDigest.getInstance("MD4");
        }
        catch (NoSuchAlgorithmException e)
        {
            Security.addProvider(new CryptixCrypto());
        }
    }

    
    public MD4PasswordEncoder()
    {
        super();
        // TODO Auto-generated constructor stub
    }

    // ~ Methods
    // ================================================================

    public boolean isPasswordValid(String encPass, String rawPass, Object salt)
    {
        String pass1 = "" + encPass;
        String pass2 = encodeInternal(mergePasswordAndSalt(rawPass, salt, false));

        return pass1.equals(pass2);
    }

    public String encodePassword(String rawPass, Object salt)
    {
        return encodeInternal(mergePasswordAndSalt(rawPass, salt, false));
    }

    private String encodeInternal(String input)
    {
        if (!getEncodeHashAsBase64())
        {
            return new String(Hex.encodeHex(md4(input)));
        }

        byte[] encoded = Base64.encodeBase64(md4(input));

        return new String(encoded);
    }

    private byte[] md4(String input)
    {
        try
        {
            MessageDigest digester = MessageDigest.getInstance("MD4");
            return digester.digest(input.getBytes());
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new RuntimeException(e.getMessage());
        }
    }

}
