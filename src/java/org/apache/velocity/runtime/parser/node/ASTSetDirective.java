/* Generated By:JJTree: Do not edit this line. ASTSetDirective.java */

/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "The Jakarta Project", "Tomcat", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */

/**
 * Node for the #set directive
 *
 * @author <a href="mailto:jvanzyl@periapt.com">Jason van Zyl</a>
 * @author <a href="mailto:geirm@optonline.net">Geir Magnusson Jr.</a>
 * @version $Id: ASTSetDirective.java,v 1.7 2000/11/11 22:40:05 geirm Exp $
 */

package org.apache.velocity.runtime.parser.node;

import java.io.IOException;
import java.io.Writer;

import org.apache.velocity.Context;
import org.apache.velocity.runtime.Runtime;
import org.apache.velocity.runtime.exception.ReferenceException;
import org.apache.velocity.runtime.parser.*;

public class ASTSetDirective extends SimpleNode
{
    private Node right;
    private ASTReference left;
    private Object value;
    
    public ASTSetDirective(int id)
    {
        super(id);
    }

    public ASTSetDirective(Parser p, int id)
    {
        super(p, id);
    }


    /** Accept the visitor. **/
    public Object jjtAccept(ParserVisitor visitor, Object data)
    {
        return visitor.visit(this, data);
    }

    /**
     *  This specfically addresses a special case :
     *  
     *  #set $foo = [ "a","b"]
     *  #foreach( $i in $foo)
     *     $i
     *  #end
     *  
     *  Otherwise, the rest is done in render()
     *
     *  We may want to revisit this. This is due to Foreach.init()'s requirement that the references
     *  be present in the context, and Foreach.init() is called before set.render().
     *  Or move all of the render() code in here...
     */
    public Object init(Context context, Object data) throws Exception
    {
        /*
         *  init the tree correctly
         */

        super.init( context, data );

        /**
         * We need to place all RHS objects into the context
         * so that subsequent introspection is performed
         * correctly.
         *
         * #set $bar = "bar"
         * $provider.concat("foo", $bar)
         *
         * will not be introspected correctly if $bar
         * is not place in the context.
         *
         * The same type of problem occurs with the
         * following:
         *
         * #set $list = ["one", "two", "three" ]
         * #foreach ($element in $list)
         *     $element
         * #end
         *
         * If $list is not placed in the context then
         * the introspection phase of the #foreach will
         * not occur as expected.
         *
         * The basic deal is that objects that are #set
         * must be placed in the context during the initialization
         * phase in order for subsequent VTL to be introspected
         * correctly.
         */
         
        right = getRightHandSide();
        value = right.value(context);
        
        if ( value != null)
        {
            left = getLeftHandSide();
           
            if (left != null && left.jjtGetNumChildren() == 0)
                context.put(left.getFirstToken().image.substring(1), value);
        }           
     
        return data;
    }        

    public boolean render(Context context, Writer writer)
        throws IOException
    {
        right = getRightHandSide();

        if (right.value(context) == null)
        {
            Runtime.error(new ReferenceException("#set", right));
            return false;
        }                

        value = right.value(context);
        left = getLeftHandSide();
        
        if (left.jjtGetNumChildren() == 0)
            context.put(left.getFirstToken().image.substring(1), value);
        else
            left.setValue(context, value);
    
        return true;
    }

    private ASTReference getLeftHandSide()
    {
        return (ASTReference) jjtGetChild(0).jjtGetChild(0).jjtGetChild(0);
    }

    private Node getRightHandSide()
    {
        return jjtGetChild(0).jjtGetChild(0).jjtGetChild(1).jjtGetChild(0);
    }
}
