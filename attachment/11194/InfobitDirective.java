/*
 * Copyright (c) 2003
 * Information Desire GmbH
 * All rights reserved.
 */
package com.infodesire.infobit.render.velocity;

import java.io.IOException;
import java.io.Writer;

import org.apache.velocity.runtime.directive.InputBase;

import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.context.Context;

import org.apache.velocity.Template;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.parser.node.Node;
import org.apache.velocity.runtime.parser.node.SimpleNode;
import org.apache.velocity.runtime.resource.Resource;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.app.event.EventCartridge;

import com.infodesire.infobit.InfobitManager;
import com.infodesire.infobit.data.Infobit;
import com.infodesire.infobit.InfobitException;

import org.apache.velocity.util.introspection.IntrospectionCacheData;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Iterator;

/**
 * pluggable directive to handle custom #infobit tag. supply infobit name or
 * infobit to it. directove will load infobit from nanager if necessary, oibtain
 * correct template from manager, setup context and merge it
 *
 * @author    konstantin
 * @created   August 22, 2003
 * @version   $Revision: 1.5 $
 */
public class InfobitDirective extends InputBase {
    /**
     * Return name of this directive.
     *
     * @return   The Name value
     */
    public String getName() {
        return "infobit";
    }


    /**
     * Return type of this directive.
     *
     * @return   The Type value
     */
    public int getType() {
        return LINE;
    }


    /**
     * renders infobit in appropriate way
     *
     * @param context                        Description of Parameter
     * @param writer                         Description of Parameter
     * @param node                           Description of Parameter
     * @return                               Description of the Returned Value
     * @exception IOException                Description of Exception
     * @exception ResourceNotFoundException  Description of Exception
     * @exception ParseErrorException        Description of Exception
     * @exception MethodInvocationException  Description of Exception
     */
    public boolean render(InternalContextAdapter context,
        Writer writer, Node node)
         throws IOException, ResourceNotFoundException, ParseErrorException,
        MethodInvocationException {

        VelocityRenderer renderer = (VelocityRenderer) rsvc.getApplicationAttribute(VelocityRenderer.class.getName());
        /*
         *  did we get an argument?
         */
        if (node.jjtGetChild(0) == null) {
            rsvc.error("#infobit() error :  null argument");
            return false;
        }

        /*
         *  does it have a value?  If you have a null reference, then no.
         */
        Object value = node.jjtGetChild(0).value(context);

        if (value == null) {
            rsvc.error("#infobit() error :  null argument");
            return false;
        }

        Infobit infobit;
        if (value instanceof Infobit) {
            infobit = (Infobit) value;
        }
        else {
            infobit = renderer.getInfobit(value.toString());
            if (infobit == null) {
                rsvc.error("infobit " + value.toString() + " could not be load by manager");
                return false;
            }
        }

        String arg = infobit.getName();

        Object[] templateStack = context.getTemplateNameStack();

        if (templateStack.length >=
            rsvc.getInt(RuntimeConstants.PARSE_DIRECTIVE_MAXDEPTH, 20)) {
            StringBuffer path = new StringBuffer();

            for (int i = 0; i < templateStack.length; ++i) {
                path.append(" > " + templateStack[i]);
            }

            rsvc.error("Max recursion depth reached (" +
                templateStack.length + ")" + " File stack:" + path);
            return false;
        }

        Template t = null;
        String tName = null;
        try {
            tName = renderer.getTemplateName(infobit);
            t = rsvc.getTemplate(tName, getInputEncoding(context));
        } catch (ResourceNotFoundException rnfe) {
            /*
             *  the arg wasn't found.  Note it and throw
             */
            rsvc.error("#infobit(): cannot find template '" + tName +
                "', called from template " +
                context.getCurrentTemplateName() + " at (" +
                getLine() + ", " + getColumn() + ")");
            throw rnfe;
        } catch (ParseErrorException pee) {
            /*
             *  the arg was found, but didn't parse - syntax error
             *  note it and throw
             */
            rsvc.error("#infobit(): syntax error in #infobit()-ed template '" +
                tName + "', called from template " +
                context.getCurrentTemplateName() + " at (" +
                getLine() + ", " + getColumn() + ")");

            throw pee;
        } catch (Exception e) {
            rsvc.error("#infobit() : arg = " + arg + ".  Exception : " + e);
            return false;
        }

        /*
         *  and render it
         */
        try {
            context.pushCurrentTemplateName(arg);

            // we wrap context into new one
            WrappedInternalContextAdapter wrapped = new WrappedInternalContextAdapter(context);
            renderer.setupContext(wrapped, infobit);
            ((SimpleNode) t.getData()).render(wrapped, writer);

        } catch (Exception e) {
            /*
             *  if it's a MIE, it came from the render.... throw it...
             */
            if (e instanceof MethodInvocationException) {
                throw (MethodInvocationException) e;
            }

            rsvc.error("Exception rendering #parse( " + arg + " )  : " + e);
            return false;
        } finally {
            context.popCurrentTemplateName();
        }

        return true;
    }


    /**
     * context wrapper for infobit context to prevent bleeding of context
     * variables and effectively manage links etc.
     *
     * @author    konstantin
     * @created   August 25, 2003
     * @version   $Revision: 1.5 $
     */
    class WrappedInternalContextAdapter implements InternalContextAdapter {
        private HashMap params = new HashMap();
        private InternalContextAdapter contextAdapter;


        /**
         * Constructor for the WrappedInternalContextAdapter object
         *
         * @param contextAdapter  Description of Parameter
         */
        public WrappedInternalContextAdapter(InternalContextAdapter contextAdapter) {
            this.contextAdapter = contextAdapter;
        }


        /**
         * Gets the BaseContext attribute of the WrappedInternalContextAdapter
         * object
         *
         * @return   The BaseContext value
         */
        public InternalContextAdapter getBaseContext() {
            return contextAdapter.getBaseContext();
        }


        /**
         * Gets the CurrentResource attribute of the
         * WrappedInternalContextAdapter object
         *
         * @return   The CurrentResource value
         */
        public Resource getCurrentResource() {
            return contextAdapter.getCurrentResource();
        }


        /**
         * Gets the CurrentTemplateName attribute of the
         * WrappedInternalContextAdapter object
         *
         * @return   The CurrentTemplateName value
         */
        public String getCurrentTemplateName() {
            return contextAdapter.getCurrentTemplateName();
        }


        /**
         * Gets the EventCartridge attribute of the
         * WrappedInternalContextAdapter object
         *
         * @return   The EventCartridge value
         */
        public EventCartridge getEventCartridge() {
            return contextAdapter.getEventCartridge();
        }


        /**
         * Gets the InternalUserContext attribute of the
         * WrappedInternalContextAdapter object
         *
         * @return   The InternalUserContext value
         */
        public Context getInternalUserContext() {
            return contextAdapter.getInternalUserContext();
        }


        /**
         * Gets the Keys attribute of the WrappedInternalContextAdapter object
         *
         * @return   The Keys value
         */
        public Object[] getKeys() {
            Set keySet = params.keySet();

            if (keySet == null) {
                return contextAdapter.getKeys();
            }

            Object[] objects = new Object[keySet.size()];
            keySet.toArray(objects);

            return objects;
        }


        /**
         * Gets the TemplateNameStack attribute of the
         * WrappedInternalContextAdapter object
         *
         * @return   The TemplateNameStack value
         */
        public Object[] getTemplateNameStack() {
            return contextAdapter.getTemplateNameStack();
        }


        /**
         * DOCUMENT METHOD
         *
         * @param s  Description of Parameter
         * @return   Description of the Returned Value
         */
        public Object get(String s) {
            Object obj = params.get(s);

            if (obj == null) {
                obj = contextAdapter.get(s);
            }

            return obj;
        }


        /**
         * Sets the CurrentResource attribute of the
         * WrappedInternalContextAdapter object
         *
         * @param resource  The new CurrentResource value
         */
        public void setCurrentResource(Resource resource) {
            contextAdapter.setCurrentResource(resource);
        }


        /**
         * DOCUMENT METHOD
         *
         * @param eventCartridge  Description of Parameter
         * @return                Description of the Returned Value
         */
        public EventCartridge attachEventCartridge(EventCartridge eventCartridge) {
            return contextAdapter.attachEventCartridge(eventCartridge);
        }


        /**
         * DOCUMENT METHOD
         *
         * @param o  Description of Parameter
         * @return   Description of the Returned Value
         */
        public boolean containsKey(Object o) {
            if (params.containsKey(o)) {
                return true;
            }

            return contextAdapter.containsKey(o);
        }


        /**
         * DOCUMENT METHOD
         *
         * @param o  Description of Parameter
         * @return   Description of the Returned Value
         */
        public IntrospectionCacheData icacheGet(Object o) {
            return contextAdapter.icacheGet(o);
        }


        /**
         * DOCUMENT METHOD
         *
         * @param o                       Description of Parameter
         * @param introspectionCacheData  Description of Parameter
         */
        public void icachePut(Object o, IntrospectionCacheData introspectionCacheData) {
            contextAdapter.icachePut(o, introspectionCacheData);
        }


        /**
         * DOCUMENT METHOD
         */
        public void popCurrentTemplateName() {
            contextAdapter.popCurrentTemplateName();
        }


        /**
         * DOCUMENT METHOD
         *
         * @param s  Description of Parameter
         */
        public void pushCurrentTemplateName(String s) {
            contextAdapter.pushCurrentTemplateName(s);
        }


        /**
         * DOCUMENT METHOD
         *
         * @param s  Description of Parameter
         * @param o  Description of Parameter
         * @return   Description of the Returned Value
         */
        public Object put(String s, Object o) {
            return params.put(s, o);
        }


        /**
         * DOCUMENT METHOD
         *
         * @param o  Description of Parameter
         * @return   Description of the Returned Value
         */
        public Object remove(Object o) {
            Object obj = params.remove(o);

            if (obj == null) {
                obj = contextAdapter.remove(o);
            }

            return obj;
        }
    }
}
