/*
 * (c) Copyright 2008, 2009 Hewlett-Packard Development Company, LP
 * All rights reserved.
 * [See end of file]
 */

package com.hp.hpl.jena.tdb.nodetable;

import java.nio.ByteBuffer;

import atlas.lib.Bytes;
import atlas.lib.StrUtils;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.sparql.sse.SSE;
import com.hp.hpl.jena.sparql.util.FmtUtils;
import com.hp.hpl.jena.tdb.TDBException;

/** Simple encoder/decoder for nodes that uses the SSE string encoding.
 *  The encoding is a length (4 bytes) and a UTF-8 string.
 *  
 *  Note that this is not compatible with UTF-8 strings written with
 *  the standard Java mechanism {@link java.io.DataInput DataInput}/
 *  {@link java.io.DataOutput DataOutput} because they are limited to
 *  64K bytes of UTF-8 data (2 byte length code).
 */

public class NodecSSE implements Nodec
{
    private static boolean SafeChars = false ;
    // Characters in IRIs that are illegal and cause SSE problems, but we wish to keep.
    final private static char MarkerChar = '_' ;
    final private static char[] invalidIRIChars = { MarkerChar , ' ' } ; 
    
    public NodecSSE() {}
    
    //@Override
    public ByteBuffer alloc(Node node)
    {
        // +4 for the length slot
        return ByteBuffer.allocate(4+maxLength(node)) ;
    }
    
    //@Override
    public int encode(Node node, ByteBuffer bb, PrefixMapping pmap)
    {
        if ( node.isURI() ) 
        {
            // IMPROVE.
            // Pesky spaces etc
            String x = StrUtils.encode(node.getURI(), MarkerChar, invalidIRIChars) ;
            if ( x != node.getURI() )
                node = Node.createURI(x) ; 
        }
        
        String str ;
        if ( node.isBlank() )
            str = "_:"+node.getBlankNodeLabel() ;
        else 
            str = FmtUtils.stringForNode(node, pmap) ;
        
        // String -> bytes
        bb.position(4) ;
        int x = Bytes.toByteBuffer(str, bb) ;
        bb.position(0) ;
        bb.putInt(x) ;      // Length in bytes
        return x+4 ;
    }

    //@Override
    public Node decode(ByteBuffer bb, PrefixMapping pmap)
    {
        // Bytes -> String
        String str = Bytes.fromByteBuffer(bb) ;
        // String -> Node

        // Do better: this is a key operation
        // HARDCODE THIS
        
        Node n = SSE.parseNode(str, pmap) ;
        if ( n.isURI() && n.getURI().indexOf(MarkerChar) >= 0 )
        {
            String uri = StrUtils.decode(n.getURI(), '_') ;
            if ( uri != n.getURI() )
                n = Node.createURI(uri) ;
        }
        return n ;
    }

    // Over-estimate the length of the encoding.
    private static int maxLength(Node node)
    {
        if ( node.isBlank() )
            // "_:"
            return 2+maxLength(node.getBlankNodeLabel()) ;    
        if ( node.isURI() )
            // "<>"
            return 2+maxLength(node.getURI()) ;
        if ( node.isLiteral() )
        {
            if ( node.getLiteralDatatypeURI() != null )
                // The quotes and also space for ^^<>
                return 2+maxLength(node.getLiteralLexicalForm())+maxLength(node.getLiteralDatatypeURI()) ;
            else if ( node.getLiteralLanguage() != null )
                // The quotes and also space for @
                return 3+maxLength(node.getLiteralLexicalForm()) ;
            else
                return 2+maxLength(node.getLiteralLexicalForm()) ;
        }
        if ( node.isVariable() )
            // "?"
            return 1+maxLength(node.getName()) ;
        throw new TDBException("Unrecognized node type: "+node) ;
    }

    private static int maxLength(String string)
    {
        // Very worse case for UTF-8 - and then some.
        // Encoding every character as _XX or bad UTF-8 conversion (3 bytes)
        // Max 3 bytes UTF-8 for up to 10FFFF (NB Java treats above 16bites as surrogate pairs only). 
        return string.length()*3 ;
    }

}

/*
 * (c) Copyright 2008, 2009 Hewlett-Packard Development Company, LP
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */