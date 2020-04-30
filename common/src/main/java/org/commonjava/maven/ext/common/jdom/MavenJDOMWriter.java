/**
 * Copyright (C) 2012 Apache Software Foundation (jdcasey@commonjava.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.maven.ext.common.jdom;

import org.apache.maven.model.*;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.jdom.Element;

import java.io.IOException;
import java.util.Iterator;


/**
 * Class MavenJDOMWriter.
 * 
 * @version $Revision$ $Date$
 */
@SuppressWarnings( "all" )
public class MavenJDOMWriter
    extends AbstractJDOMWriter<Model, MavenJDOMWriter>
{
    public MavenJDOMWriter()
    {
    }
//
//    public MavenJDOMWriter( final String encoding )
//    {
//        super( encoding );
//    }
//
//    public MavenJDOMWriter( final Model model )
//    {
//        super( model.getModelEncoding() == null ? "UTF-8" : model.getModelEncoding() );
//    }

    /**
     * Method iterateContributor.
     * 
     * @param counter
     * @param childTag
     * @param parentTag
     * @param list
     * @param parent
     */
    protected void iterateContributor( final IndentationCounter counter, final Element parent,
                                       final java.util.Collection list, final String parentTag,
                                       final String childTag )
    {
        final boolean shouldExist = ( list != null ) && ( list.size() > 0 );
        final Element element = Utils.updateElement( counter, parent, parentTag, shouldExist );
        if ( shouldExist )
        {
            final Iterator it = list.iterator();
            Iterator elIt = element.getChildren( childTag, element.getNamespace() ).iterator();
            if ( !elIt.hasNext() )
            {
                elIt = null;
            }
            final IndentationCounter innerCount = new IndentationCounter( counter.getDepth() + 1 );
            while ( it.hasNext() )
            {
                final Contributor value = (Contributor) it.next();
                Element el;
                if ( elIt != null && elIt.hasNext() )
                {
                    el = (Element) elIt.next();
                    if ( !elIt.hasNext() )
                    {
                        elIt = null;
                    }
                }
                else
                {
                    el = factory.element( childTag, element.getNamespace() );
                    Utils.insertAtPreferredLocation( element, el, innerCount );
                }
                updateContributor( value, childTag, innerCount, el );
                innerCount.increaseCount();
            }
            if ( elIt != null )
            {
                while ( elIt.hasNext() )
                {
                    elIt.next();
                    elIt.remove();
                }
            }
        }
    } // -- void iterateContributor( Counter, Element, java.util.Collection, java.lang.String, java.lang.String )

    /**
     * Method iterateDependency.
     * 
     * @param counter
     * @param childTag
     * @param parentTag
     * @param list
     * @param parent
     */
    protected void iterateDependency( final IndentationCounter counter, final Element parent,
                                      final java.util.Collection list, final String parentTag,
                                      final String childTag )
    {
        final boolean shouldExist = ( list != null ) && ( list.size() > 0 );
        final Element element = Utils.updateElement( counter, parent, parentTag, shouldExist );
        if ( shouldExist )
        {
            final Iterator it = list.iterator();
            Iterator elIt = element.getChildren( childTag, element.getNamespace() ).iterator();
            if ( !elIt.hasNext() )
            {
                elIt = null;
            }
            final IndentationCounter innerCount = new IndentationCounter( counter.getDepth() + 1 );
            while ( it.hasNext() )
            {
                final Dependency value = (Dependency) it.next();
                Element el;
                if ( elIt != null && elIt.hasNext() )
                {
                    el = (Element) elIt.next();
                    if ( !elIt.hasNext() )
                    {
                        elIt = null;
                    }
                }
                else
                {
                    el = factory.element( childTag, element.getNamespace() );
                    Utils.insertAtPreferredLocation( element, el, innerCount );
                }
                updateDependency( value, childTag, innerCount, el );
                innerCount.increaseCount();
            }
            if ( elIt != null )
            {
                while ( elIt.hasNext() )
                {
                    elIt.next();
                    elIt.remove();
                }
            }
        }
    } // -- void iterateDependency( Counter, Element, java.util.Collection, java.lang.String, java.lang.String )

    /**
     * Method iterateDeveloper.
     * 
     * @param counter
     * @param childTag
     * @param parentTag
     * @param list
     * @param parent
     */
    protected void iterateDeveloper( final IndentationCounter counter, final Element parent,
                                     final java.util.Collection list, final String parentTag,
                                     final String childTag )
    {
        final boolean shouldExist = ( list != null ) && ( list.size() >= 0 );
        final Element element = Utils.updateElement( counter, parent, parentTag, shouldExist );
        if ( shouldExist )
        {
            final Iterator it = list.iterator();
            Iterator elIt = element.getChildren( childTag, element.getNamespace() ).iterator();
            if ( !elIt.hasNext() )
            {
                elIt = null;
            }
            final IndentationCounter innerCount = new IndentationCounter( counter.getDepth() + 1 );
            while ( it.hasNext() )
            {
                final Developer value = (Developer) it.next();
                Element el;
                if ( elIt != null && elIt.hasNext() )
                {
                    el = (Element) elIt.next();
                    if ( !elIt.hasNext() )
                    {
                        elIt = null;
                    }
                }
                else
                {
                    el = factory.element( childTag, element.getNamespace() );
                    Utils.insertAtPreferredLocation( element, el, innerCount );
                }
                updateDeveloper( value, childTag, innerCount, el );
                innerCount.increaseCount();
            }
            if ( elIt != null )
            {
                while ( elIt.hasNext() )
                {
                    elIt.next();
                    elIt.remove();
                }
            }
        }
    } // -- void iterateDeveloper( Counter, Element, java.util.Collection, java.lang.String, java.lang.String )

    /**
     * Method iterateExclusion.
     * 
     * @param counter
     * @param childTag
     * @param parentTag
     * @param list
     * @param parent
     */
    protected void iterateExclusion( final IndentationCounter counter, final Element parent,
                                     final java.util.Collection list, final String parentTag,
                                     final String childTag )
    {
        final boolean shouldExist = ( list != null ) && ( list.size() > 0 );
        final Element element = Utils.updateElement( counter, parent, parentTag, shouldExist );
        if ( shouldExist )
        {
            final Iterator it = list.iterator();
            Iterator elIt = element.getChildren( childTag, element.getNamespace() ).iterator();
            if ( !elIt.hasNext() )
            {
                elIt = null;
            }
            final IndentationCounter innerCount = new IndentationCounter( counter.getDepth() + 1 );
            while ( it.hasNext() )
            {
                final Exclusion value = (Exclusion) it.next();
                Element el;
                if ( elIt != null && elIt.hasNext() )
                {
                    el = (Element) elIt.next();
                    if ( !elIt.hasNext() )
                    {
                        elIt = null;
                    }
                }
                else
                {
                    el = factory.element( childTag, element.getNamespace() );
                    Utils.insertAtPreferredLocation( element, el, innerCount );
                }
                updateExclusion( value, childTag, innerCount, el );
                innerCount.increaseCount();
            }
            if ( elIt != null )
            {
                while ( elIt.hasNext() )
                {
                    elIt.next();
                    elIt.remove();
                }
            }
        }
    } // -- void iterateExclusion( Counter, Element, java.util.Collection, java.lang.String, java.lang.String )

    /**
     * Method iterateExtension.
     * 
     * @param counter
     * @param childTag
     * @param parentTag
     * @param list
     * @param parent
     */
    protected void iterateExtension( final IndentationCounter counter, final Element parent,
                                     final java.util.Collection list, final String parentTag,
                                     final String childTag )
    {
        final boolean shouldExist = ( list != null ) && ( list.size() > 0 );
        final Element element = Utils.updateElement( counter, parent, parentTag, shouldExist );
        if ( shouldExist )
        {
            final Iterator it = list.iterator();
            Iterator elIt = element.getChildren( childTag, element.getNamespace() ).iterator();
            if ( !elIt.hasNext() )
            {
                elIt = null;
            }
            final IndentationCounter innerCount = new IndentationCounter( counter.getDepth() + 1 );
            while ( it.hasNext() )
            {
                final Extension value = (Extension) it.next();
                Element el;
                if ( elIt != null && elIt.hasNext() )
                {
                    el = (Element) elIt.next();
                    if ( !elIt.hasNext() )
                    {
                        elIt = null;
                    }
                }
                else
                {
                    el = factory.element( childTag, element.getNamespace() );
                    Utils.insertAtPreferredLocation( element, el, innerCount );
                }
                updateExtension( value, childTag, innerCount, el );
                innerCount.increaseCount();
            }
            if ( elIt != null )
            {
                while ( elIt.hasNext() )
                {
                    elIt.next();
                    elIt.remove();
                }
            }
        }
    } // -- void iterateExtension( Counter, Element, java.util.Collection, java.lang.String, java.lang.String )

    /**
     * Method iterateLicense.
     * 
     * @param counter
     * @param childTag
     * @param parentTag
     * @param list
     * @param parent
     */
    protected void iterateLicense( final IndentationCounter counter, final Element parent,
                                   final java.util.Collection list, final String parentTag,
                                   final String childTag )
    {
        final boolean shouldExist = ( list != null ) && ( list.size() > 0 );
        final Element element = Utils.updateElement( counter, parent, parentTag, shouldExist );
        if ( shouldExist )
        {
            final Iterator it = list.iterator();
            Iterator elIt = element.getChildren( childTag, element.getNamespace() ).iterator();
            if ( !elIt.hasNext() )
            {
                elIt = null;
            }
            final IndentationCounter innerCount = new IndentationCounter( counter.getDepth() + 1 );
            while ( it.hasNext() )
            {
                final License value = (License) it.next();
                Element el;
                if ( elIt != null && elIt.hasNext() )
                {
                    el = (Element) elIt.next();
                    if ( !elIt.hasNext() )
                    {
                        elIt = null;
                    }
                }
                else
                {
                    el = factory.element( childTag, element.getNamespace() );
                    Utils.insertAtPreferredLocation( element, el, innerCount );
                }
                updateLicense( value, childTag, innerCount, el );
                innerCount.increaseCount();
            }
            if ( elIt != null )
            {
                while ( elIt.hasNext() )
                {
                    elIt.next();
                    elIt.remove();
                }
            }
        }
    } // -- void iterateLicense( Counter, Element, java.util.Collection, java.lang.String, java.lang.String )

    /**
     * Method iterateMailingList.
     * 
     * @param counter
     * @param childTag
     * @param parentTag
     * @param list
     * @param parent
     */
    protected void iterateMailingList( final IndentationCounter counter, final Element parent,
                                       final java.util.Collection list, final String parentTag,
                                       final String childTag )
    {
        final boolean shouldExist = ( list != null ) && ( list.size() > 0 );
        final Element element = Utils.updateElement( counter, parent, parentTag, shouldExist );
        if ( shouldExist )
        {
            final Iterator it = list.iterator();
            Iterator elIt = element.getChildren( childTag, element.getNamespace() ).iterator();
            if ( !elIt.hasNext() )
            {
                elIt = null;
            }
            final IndentationCounter innerCount = new IndentationCounter( counter.getDepth() + 1 );
            while ( it.hasNext() )
            {
                final MailingList value = (MailingList) it.next();
                Element el;
                if ( elIt != null && elIt.hasNext() )
                {
                    el = (Element) elIt.next();
                    if ( !elIt.hasNext() )
                    {
                        elIt = null;
                    }
                }
                else
                {
                    el = factory.element( childTag, element.getNamespace() );
                    Utils.insertAtPreferredLocation( element, el, innerCount );
                }
                updateMailingList( value, childTag, innerCount, el );
                innerCount.increaseCount();
            }
            if ( elIt != null )
            {
                while ( elIt.hasNext() )
                {
                    elIt.next();
                    elIt.remove();
                }
            }
        }
    } // -- void iterateMailingList( Counter, Element, java.util.Collection, java.lang.String, java.lang.String )

    /**
     * Method iterateNotifier.
     * 
     * @param counter
     * @param childTag
     * @param parentTag
     * @param list
     * @param parent
     */
    protected void iterateNotifier( final IndentationCounter counter, final Element parent,
                                    final java.util.Collection list, final String parentTag,
                                    final String childTag )
    {
        final boolean shouldExist = ( list != null ) && ( list.size() > 0 );
        final Element element = Utils.updateElement( counter, parent, parentTag, shouldExist );
        if ( shouldExist )
        {
            final Iterator it = list.iterator();
            Iterator elIt = element.getChildren( childTag, element.getNamespace() ).iterator();
            if ( !elIt.hasNext() )
            {
                elIt = null;
            }
            final IndentationCounter innerCount = new IndentationCounter( counter.getDepth() + 1 );
            while ( it.hasNext() )
            {
                final Notifier value = (Notifier) it.next();
                Element el;
                if ( elIt != null && elIt.hasNext() )
                {
                    el = (Element) elIt.next();
                    if ( !elIt.hasNext() )
                    {
                        elIt = null;
                    }
                }
                else
                {
                    el = factory.element( childTag, element.getNamespace() );
                    Utils.insertAtPreferredLocation( element, el, innerCount );
                }
                updateNotifier( value, childTag, innerCount, el );
                innerCount.increaseCount();
            }
            if ( elIt != null )
            {
                while ( elIt.hasNext() )
                {
                    elIt.next();
                    elIt.remove();
                }
            }
        }
    } // -- void iterateNotifier( Counter, Element, java.util.Collection, java.lang.String, java.lang.String )

    /**
     * Method iteratePlugin.
     * 
     * @param counter
     * @param childTag
     * @param parentTag
     * @param list
     * @param parent
     */
    protected void iteratePlugin( final IndentationCounter counter, final Element parent,
                                  final java.util.Collection list, final String parentTag,
                                  final String childTag )
    {
        final boolean shouldExist = ( list != null ) && ( list.size() > 0 );
        final Element element = Utils.updateElement( counter, parent, parentTag, shouldExist );
        if ( shouldExist )
        {
            final Iterator it = list.iterator();
            Iterator elIt = element.getChildren( childTag, element.getNamespace() ).iterator();
            if ( !elIt.hasNext() )
            {
                elIt = null;
            }
            final IndentationCounter innerCount = new IndentationCounter( counter.getDepth() + 1 );
            while ( it.hasNext() )
            {
                final Plugin value = (Plugin) it.next();
                Element el;
                if ( elIt != null && elIt.hasNext() )
                {
                    el = (Element) elIt.next();
                    if ( !elIt.hasNext() )
                    {
                        elIt = null;
                    }
                }
                else
                {
                    el = factory.element( childTag, element.getNamespace() );
                    Utils.insertAtPreferredLocation( element, el, innerCount );
                }
                updatePlugin( value, childTag, innerCount, el );
                innerCount.increaseCount();
            }
            if ( elIt != null )
            {
                while ( elIt.hasNext() )
                {
                    elIt.next();
                    elIt.remove();
                }
            }
        }
    } // -- void iteratePlugin( Counter, Element, java.util.Collection, java.lang.String, java.lang.String )

    /**
     * Method iteratePluginExecution.
     * 
     * @param counter
     * @param childTag
     * @param parentTag
     * @param list
     * @param parent
     */
    protected void iteratePluginExecution( final IndentationCounter counter, final Element parent,
                                           final java.util.Collection list, final String parentTag,
                                           final String childTag )
    {
        final boolean shouldExist = ( list != null ) && ( list.size() > 0 );
        final Element element = Utils.updateElement( counter, parent, parentTag, shouldExist );
        if ( shouldExist )
        {
            final Iterator it = list.iterator();
            Iterator elIt = element.getChildren( childTag, element.getNamespace() ).iterator();
            if ( !elIt.hasNext() )
            {
                elIt = null;
            }
            final IndentationCounter innerCount = new IndentationCounter( counter.getDepth() + 1 );
            while ( it.hasNext() )
            {
                final PluginExecution value = (PluginExecution) it.next();
                Element el;
                if ( elIt != null && elIt.hasNext() )
                {
                    el = (Element) elIt.next();
                    if ( !elIt.hasNext() )
                    {
                        elIt = null;
                    }
                }
                else
                {
                    el = factory.element( childTag, element.getNamespace() );
                    Utils.insertAtPreferredLocation( element, el, innerCount );
                }
                updatePluginExecution( value, childTag, innerCount, el );
                innerCount.increaseCount();
            }
            if ( elIt != null )
            {
                while ( elIt.hasNext() )
                {
                    elIt.next();
                    elIt.remove();
                }
            }
        }
    } // -- void iteratePluginExecution( Counter, Element, java.util.Collection, java.lang.String, java.lang.String )

    /**
     * Method iterateProfile.
     * 
     * @param counter
     * @param childTag
     * @param parentTag
     * @param list
     * @param parent
     */
    protected void iterateProfile( final IndentationCounter counter, final Element parent,
                                   final java.util.Collection list, final String parentTag,
                                   final String childTag )
    {
        final boolean shouldExist = ( list != null ) && ( list.size() > 0 );
        final Element element = Utils.updateElement( counter, parent, parentTag, shouldExist );
        if ( shouldExist )
        {
            final Iterator it = list.iterator();
            Iterator elIt = element.getChildren( childTag, element.getNamespace() ).iterator();
            if ( !elIt.hasNext() )
            {
                elIt = null;
            }
            final IndentationCounter innerCount = new IndentationCounter( counter.getDepth() + 1 );
            while ( it.hasNext() )
            {
                final Profile value = (Profile) it.next();
                Element el;
                if ( elIt != null && elIt.hasNext() )
                {
                    el = (Element) elIt.next();
                    if ( !elIt.hasNext() )
                    {
                        elIt = null;
                    }
                }
                else
                {
                    el = factory.element( childTag, element.getNamespace() );
                    Utils.insertAtPreferredLocation( element, el, innerCount );
                }
                updateProfile( value, childTag, innerCount, el );
                innerCount.increaseCount();
            }
            if ( elIt != null )
            {
                while ( elIt.hasNext() )
                {
                    elIt.next();
                    elIt.remove();
                }
            }
        }
    } // -- void iterateProfile( Counter, Element, java.util.Collection, java.lang.String, java.lang.String )

    /**
     * Method iterateReportPlugin.
     * 
     * @param counter
     * @param childTag
     * @param parentTag
     * @param list
     * @param parent
     */
    protected void iterateReportPlugin( final IndentationCounter counter, final Element parent,
                                        final java.util.Collection list, final String parentTag,
                                        final String childTag )
    {
        final boolean shouldExist = ( list != null ) && ( list.size() > 0 );
        final Element element = Utils.updateElement( counter, parent, parentTag, shouldExist );
        if ( shouldExist )
        {
            final Iterator it = list.iterator();
            Iterator elIt = element.getChildren( childTag, element.getNamespace() ).iterator();
            if ( !elIt.hasNext() )
            {
                elIt = null;
            }
            final IndentationCounter innerCount = new IndentationCounter( counter.getDepth() + 1 );
            while ( it.hasNext() )
            {
                final ReportPlugin value = (ReportPlugin) it.next();
                Element el;
                if ( elIt != null && elIt.hasNext() )
                {
                    el = (Element) elIt.next();
                    if ( !elIt.hasNext() )
                    {
                        elIt = null;
                    }
                }
                else
                {
                    el = factory.element( childTag, element.getNamespace() );
                    Utils.insertAtPreferredLocation( element, el, innerCount );
                }
                updateReportPlugin( value, childTag, innerCount, el );
                innerCount.increaseCount();
            }
            if ( elIt != null )
            {
                while ( elIt.hasNext() )
                {
                    elIt.next();
                    elIt.remove();
                }
            }
        }
    } // -- void iterateReportPlugin( Counter, Element, java.util.Collection, java.lang.String, java.lang.String )

    /**
     * Method iterateReportSet.
     * 
     * @param counter
     * @param childTag
     * @param parentTag
     * @param list
     * @param parent
     */
    protected void iterateReportSet( final IndentationCounter counter, final Element parent,
                                     final java.util.Collection list, final String parentTag,
                                     final String childTag )
    {
        final boolean shouldExist = ( list != null ) && ( list.size() > 0 );
        final Element element = Utils.updateElement( counter, parent, parentTag, shouldExist );
        if ( shouldExist )
        {
            final Iterator it = list.iterator();
            Iterator elIt = element.getChildren( childTag, element.getNamespace() ).iterator();
            if ( !elIt.hasNext() )
            {
                elIt = null;
            }
            final IndentationCounter innerCount = new IndentationCounter( counter.getDepth() + 1 );
            while ( it.hasNext() )
            {
                final ReportSet value = (ReportSet) it.next();
                Element el;
                if ( elIt != null && elIt.hasNext() )
                {
                    el = (Element) elIt.next();
                    if ( !elIt.hasNext() )
                    {
                        elIt = null;
                    }
                }
                else
                {
                    el = factory.element( childTag, element.getNamespace() );
                    Utils.insertAtPreferredLocation( element, el, innerCount );
                }
                updateReportSet( value, childTag, innerCount, el );
                innerCount.increaseCount();
            }
            if ( elIt != null )
            {
                while ( elIt.hasNext() )
                {
                    elIt.next();
                    elIt.remove();
                }
            }
        }
    } // -- void iterateReportSet( Counter, Element, java.util.Collection, java.lang.String, java.lang.String )

    /**
     * Method iterateRepository.
     * 
     * @param counter
     * @param childTag
     * @param parentTag
     * @param list
     * @param parent
     */
    protected void iterateRepository( final IndentationCounter counter, final Element parent,
                                      final java.util.Collection list, final String parentTag,
                                      final String childTag )
    {
        final boolean shouldExist = ( list != null ) && ( list.size() > 0 );
        final Element element = Utils.updateElement( counter, parent, parentTag, shouldExist );
        if ( shouldExist )
        {
            final Iterator it = list.iterator();
            Iterator elIt = element.getChildren( childTag, element.getNamespace() ).iterator();
            if ( !elIt.hasNext() )
            {
                elIt = null;
            }
            final IndentationCounter innerCount = new IndentationCounter( counter.getDepth() + 1 );
            while ( it.hasNext() )
            {
                final Repository value = (Repository) it.next();
                Element el;
                if ( elIt != null && elIt.hasNext() )
                {
                    el = (Element) elIt.next();
                    if ( !elIt.hasNext() )
                    {
                        elIt = null;
                    }
                }
                else
                {
                    el = factory.element( childTag, element.getNamespace() );
                    Utils.insertAtPreferredLocation( element, el, innerCount );
                }
                updateRepository( value, childTag, innerCount, el );
                innerCount.increaseCount();
            }
            if ( elIt != null )
            {
                while ( elIt.hasNext() )
                {
                    elIt.next();
                    elIt.remove();
                }
            }
        }
    } // -- void iterateRepository( Counter, Element, java.util.Collection, java.lang.String, java.lang.String )

    /**
     * Method iterateResource.
     * 
     * @param counter
     * @param childTag
     * @param parentTag
     * @param list
     * @param parent
     */
    protected void iterateResource( final IndentationCounter counter, final Element parent,
                                    final java.util.Collection list, final String parentTag,
                                    final String childTag )
    {
        final boolean shouldExist = ( list != null ) && ( list.size() > 0 );
        final Element element = Utils.updateElement( counter, parent, parentTag, shouldExist );
        if ( shouldExist )
        {
            final Iterator it = list.iterator();
            Iterator elIt = element.getChildren( childTag, element.getNamespace() ).iterator();
            if ( !elIt.hasNext() )
            {
                elIt = null;
            }
            final IndentationCounter innerCount = new IndentationCounter( counter.getDepth() + 1 );
            while ( it.hasNext() )
            {
                final Resource value = (Resource) it.next();
                Element el;
                if ( elIt != null && elIt.hasNext() )
                {
                    el = (Element) elIt.next();
                    if ( !elIt.hasNext() )
                    {
                        elIt = null;
                    }
                }
                else
                {
                    el = factory.element( childTag, element.getNamespace() );
                    Utils.insertAtPreferredLocation( element, el, innerCount );
                }
                updateResource( value, childTag, innerCount, el );
                innerCount.increaseCount();
            }
            if ( elIt != null )
            {
                while ( elIt.hasNext() )
                {
                    elIt.next();
                    elIt.remove();
                }
            }
        }
    } // -- void iterateResource( Counter, Element, java.util.Collection, java.lang.String, java.lang.String )

    /**
     * Method updateActivation.
     * 
     * @param activation
     * @param element
     * @param counter
     * @param xmlTag
     */
    protected void updateActivation( final Activation activation, final String xmlTag,
                                     final IndentationCounter counter, final Element element )
    {
        final boolean shouldExist = ( activation != null );
        final Element root = Utils.updateElement( counter, element, xmlTag, shouldExist );
        if ( shouldExist )
        {
            final IndentationCounter innerCount = new IndentationCounter( counter.getDepth() + 1 );
            Utils.findAndReplaceSimpleElement( innerCount,
                                         root,
                                         "activeByDefault",
                                         activation.isActiveByDefault() == false ? null
                                                         : String.valueOf( activation.isActiveByDefault() ),
                                         "false" );
            Utils.findAndReplaceSimpleElement( innerCount,
                                         root,
                                         "jdk",
                                         activation.getJdk() == null ? null : activation.getJdk(),
                                         null );
            updateActivationOS( activation.getOs(), "os", innerCount, root );
            updateActivationProperty( activation.getProperty(), "property", innerCount, root );
            updateActivationFile( activation.getFile(), "file", innerCount, root );
        }
    } // -- void updateActivation( Activation, String, Counter, Element )

    /**
     * Method updateActivationFile.
     * 
     * @param activationFile
     * @param element
     * @param counter
     * @param xmlTag
     */
    protected void updateActivationFile( final ActivationFile activationFile, final String xmlTag,
                                         final IndentationCounter counter, final Element element )
    {
        final boolean shouldExist = ( activationFile != null );
        final Element root = Utils.updateElement( counter, element, xmlTag, shouldExist );
        if ( shouldExist )
        {
            final IndentationCounter innerCount = new IndentationCounter( counter.getDepth() + 1 );
            Utils.findAndReplaceSimpleElement( innerCount, root, "missing", activationFile.getMissing() == null ? null
                            : activationFile.getMissing(), null );
            Utils.findAndReplaceSimpleElement( innerCount, root, "exists", activationFile.getExists() == null ? null
                            : activationFile.getExists(), null );
        }
    } // -- void updateActivationFile( ActivationFile, String, Counter, Element )

    /**
     * Method updateActivationOS.
     * 
     * @param activationOS
     * @param element
     * @param counter
     * @param xmlTag
     */
    protected void updateActivationOS( final ActivationOS activationOS, final String xmlTag,
                                       final IndentationCounter counter, final Element element )
    {
        final boolean shouldExist = ( activationOS != null );
        final Element root = Utils.updateElement( counter, element, xmlTag, shouldExist );
        if ( shouldExist )
        {
            final IndentationCounter innerCount = new IndentationCounter( counter.getDepth() + 1 );
            Utils.findAndReplaceSimpleElement( innerCount,
                                         root,
                                         "name",
                                         activationOS.getName() == null ? null : activationOS.getName(),
                                         null );
            Utils.findAndReplaceSimpleElement( innerCount, root, "family", activationOS.getFamily() == null ? null
                            : activationOS.getFamily(), null );
            Utils.findAndReplaceSimpleElement( innerCount,
                                         root,
                                         "arch",
                                         activationOS.getArch() == null ? null : activationOS.getArch(),
                                         null );
            Utils.findAndReplaceSimpleElement( innerCount, root, "version", activationOS.getVersion() == null ? null
                            : activationOS.getVersion(), null );
        }
    } // -- void updateActivationOS( ActivationOS, String, Counter, Element )

    /**
     * Method updateActivationProperty.
     * 
     * @param activationProperty
     * @param element
     * @param counter
     * @param xmlTag
     */
    protected void updateActivationProperty( final ActivationProperty activationProperty, final String xmlTag,
                                             final IndentationCounter counter, final Element element )
    {
        final boolean shouldExist = ( activationProperty != null );
        final Element root = Utils.updateElement( counter, element, xmlTag, shouldExist );
        if ( shouldExist )
        {
            final IndentationCounter innerCount = new IndentationCounter( counter.getDepth() + 1 );
            Utils.findAndReplaceSimpleElement( innerCount, root, "name", activationProperty.getName() == null ? null
                            : activationProperty.getName(), null );
            Utils.findAndReplaceSimpleElement( innerCount, root, "value", activationProperty.getValue() == null ? null
                            : activationProperty.getValue(), null );
        }
    } // -- void updateActivationProperty( ActivationProperty, String, Counter, Element )

    /**
     * Method updateBuild.
     * 
     * @param build
     * @param element
     * @param counter
     * @param xmlTag
     */
    protected void updateBuild( final Build build, final String xmlTag, final IndentationCounter counter,
                                final Element element )
    {
        final boolean shouldExist = ( build != null );
        final Element root = Utils.updateElement( counter, element, xmlTag, shouldExist );
        if ( shouldExist )
        {
            final IndentationCounter innerCount = new IndentationCounter( counter.getDepth() + 1 );
            Utils.findAndReplaceSimpleElement( innerCount, root, "sourceDirectory", build.getSourceDirectory() == null ? null
                            : build.getSourceDirectory(), null );
            Utils.findAndReplaceSimpleElement( innerCount,
                                         root,
                                         "scriptSourceDirectory",
                                         build.getScriptSourceDirectory() == null ? null
                                                         : build.getScriptSourceDirectory(),
                                         null );
            Utils.findAndReplaceSimpleElement( innerCount,
                                         root,
                                         "testSourceDirectory",
                                         build.getTestSourceDirectory() == null ? null : build.getTestSourceDirectory(),
                                         null );
            Utils.findAndReplaceSimpleElement( innerCount, root, "outputDirectory", build.getOutputDirectory() == null ? null
                            : build.getOutputDirectory(), null );
            Utils.findAndReplaceSimpleElement( innerCount,
                                         root,
                                         "testOutputDirectory",
                                         build.getTestOutputDirectory() == null ? null : build.getTestOutputDirectory(),
                                         null );
            iterateExtension( innerCount, root, build.getExtensions(), "extensions", "extension" );
            Utils.findAndReplaceSimpleElement( innerCount,
                                         root,
                                         "defaultGoal",
                                         build.getDefaultGoal() == null ? null : build.getDefaultGoal(),
                                         null );
            iterateResource( innerCount, root, build.getResources(), "resources", "resource" );
            iterateResource( innerCount, root, build.getTestResources(), "testResources", "testResource" );
            Utils.findAndReplaceSimpleElement( innerCount,
                                         root,
                                         "directory",
                                         build.getDirectory() == null ? null : build.getDirectory(),
                                         null );
            Utils.findAndReplaceSimpleElement( innerCount,
                                         root,
                                         "finalName",
                                         build.getFinalName() == null ? null : build.getFinalName(),
                                         null );
            Utils.findAndReplaceSimpleLists( innerCount, root, build.getFilters(), "filters", "filter" );
            updatePluginManagement( build.getPluginManagement(), "pluginManagement", innerCount, root );
            iteratePlugin( innerCount, root, build.getPlugins(), "plugins", "plugin" );
        }
    } // -- void updateBuild( Build, String, Counter, Element )

    /**
     * Method updateBuildBase.
     * 
     * @param buildBase
     * @param element
     * @param counter
     * @param xmlTag
     */
    protected void updateBuildBase( final BuildBase buildBase, final String xmlTag, final IndentationCounter counter,
                                    final Element element )
    {
        final boolean shouldExist = ( buildBase != null );
        final Element root = Utils.updateElement( counter, element, xmlTag, shouldExist );
        if ( shouldExist )
        {
            final IndentationCounter innerCount = new IndentationCounter( counter.getDepth() + 1 );
            Utils.findAndReplaceSimpleElement( innerCount, root, "defaultGoal", buildBase.getDefaultGoal() == null ? null
                            : buildBase.getDefaultGoal(), null );
            iterateResource( innerCount, root, buildBase.getResources(), "resources", "resource" );
            iterateResource( innerCount, root, buildBase.getTestResources(), "testResources", "testResource" );
            Utils.findAndReplaceSimpleElement( innerCount, root, "directory", buildBase.getDirectory() == null ? null
                            : buildBase.getDirectory(), null );
            Utils.findAndReplaceSimpleElement( innerCount, root, "finalName", buildBase.getFinalName() == null ? null
                            : buildBase.getFinalName(), null );
            Utils.findAndReplaceSimpleLists( innerCount, root, buildBase.getFilters(), "filters", "filter" );
            updatePluginManagement( buildBase.getPluginManagement(), "pluginManagement", innerCount, root );
            iteratePlugin( innerCount, root, buildBase.getPlugins(), "plugins", "plugin" );
        }
    } // -- void updateBuildBase( BuildBase, String, Counter, Element )

    /**
     * Method updateCiManagement.
     * 
     * @param ciManagement
     * @param element
     * @param counter
     * @param xmlTag
     */
    protected void updateCiManagement( final CiManagement ciManagement, final String xmlTag,
                                       final IndentationCounter counter, final Element element )
    {
        final boolean shouldExist = ( ciManagement != null );
        final Element root = Utils.updateElement( counter, element, xmlTag, shouldExist );
        if ( shouldExist )
        {
            final IndentationCounter innerCount = new IndentationCounter( counter.getDepth() + 1 );
            Utils.findAndReplaceSimpleElement( innerCount, root, "system", ciManagement.getSystem() == null ? null
                            : ciManagement.getSystem(), null );
            Utils.findAndReplaceSimpleElement( innerCount,
                                         root,
                                         "url",
                                         ciManagement.getUrl() == null ? null : ciManagement.getUrl(),
                                         null );
            iterateNotifier( innerCount, root, ciManagement.getNotifiers(), "notifiers", "notifier" );
        }
    } // -- void updateCiManagement( CiManagement, String, Counter, Element )

    /**
     * Method updateConfigurationContainer.
     * 
     * @param configurationContainer
     * @param element
     * @param counter
     * @param xmlTag
     */
    protected void updateConfigurationContainer( final ConfigurationContainer configurationContainer,
                                                 final String xmlTag, final IndentationCounter counter,
                                                 final Element element )
    {
        final boolean shouldExist = ( configurationContainer != null );
        final Element root = Utils.updateElement( counter, element, xmlTag, shouldExist );
        if ( shouldExist )
        {
            final IndentationCounter innerCount = new IndentationCounter( counter.getDepth() + 1 );
            Utils.findAndReplaceSimpleElement( innerCount,
                                         root,
                                         "inherited",
                                         configurationContainer.getInherited() == null ? null
                                                         : configurationContainer.getInherited(),
                                         null );
            Utils.findAndReplaceXpp3DOM( innerCount,
                                   root,
                                   "configuration",
                                   (Xpp3Dom) configurationContainer.getConfiguration() );
        }
    } // -- void updateConfigurationContainer( ConfigurationContainer, String, Counter, Element )

    /**
     * Method updateContributor.
     * 
     * @param contributor
     * @param element
     * @param counter
     * @param xmlTag
     */
    protected void updateContributor( final Contributor contributor, final String xmlTag,
                                      final IndentationCounter counter, final Element element )
    {
        final Element root = element;
        final IndentationCounter innerCount = new IndentationCounter( counter.getDepth() + 1 );
        Utils.findAndReplaceSimpleElement( innerCount,
                                     root,
                                     "name",
                                     contributor.getName() == null ? null : contributor.getName(),
                                     null );
        Utils.findAndReplaceSimpleElement( innerCount,
                                     root,
                                     "email",
                                     contributor.getEmail() == null ? null : contributor.getEmail(),
                                     null );
        Utils.findAndReplaceSimpleElement( innerCount,
                                     root,
                                     "url",
                                     contributor.getUrl() == null ? null : contributor.getUrl(),
                                     null );
        Utils.findAndReplaceSimpleElement( innerCount, root, "organization", contributor.getOrganization() == null ? null
                        : contributor.getOrganization(), null );
        Utils.findAndReplaceSimpleElement( innerCount,
                                     root,
                                     "organizationUrl",
                                     contributor.getOrganizationUrl() == null ? null : contributor.getOrganizationUrl(),
                                     null );
        Utils.findAndReplaceSimpleLists( innerCount, root, contributor.getRoles(), "roles", "role" );
        Utils.findAndReplaceSimpleElement( innerCount, root, "timezone", contributor.getTimezone() == null ? null
                        : contributor.getTimezone(), null );
        Utils.findAndReplaceProperties( innerCount, root, "properties", contributor.getProperties() );
    } // -- void updateContributor( Contributor, String, Counter, Element )

    /**
     * Method updateDependency.
     * 
     * @param dependency
     * @param element
     * @param counter
     * @param xmlTag
     */
    protected void updateDependency( final Dependency dependency, final String xmlTag,
                                     final IndentationCounter counter, final Element element )
    {
        final Element root = element;
        final IndentationCounter innerCount = new IndentationCounter( counter.getDepth() + 1 );
        Utils.findAndReplaceSimpleElement( innerCount,
                                     root,
                                     "groupId",
                                     dependency.getGroupId() == null ? null : dependency.getGroupId(),
                                     null );
        Utils.findAndReplaceSimpleElement( innerCount, root, "artifactId", dependency.getArtifactId() == null ? null
                        : dependency.getArtifactId(), null );
        Utils.findAndReplaceSimpleElement( innerCount,
                                     root,
                                     "version",
                                     dependency.getVersion() == null ? null : dependency.getVersion(),
                                     null );
        Utils.findAndReplaceSimpleElement( innerCount,
                                     root,
                                     "type",
                                     dependency.getType() == null ? null : dependency.getType(),
                                     "jar" );
        Utils.findAndReplaceSimpleElement( innerCount, root, "classifier", dependency.getClassifier() == null ? null
                        : dependency.getClassifier(), null );
        Utils.findAndReplaceSimpleElement( innerCount,
                                     root,
                                     "scope",
                                     dependency.getScope() == null ? null : dependency.getScope(),
                                     null );
        Utils.findAndReplaceSimpleElement( innerCount, root, "systemPath", dependency.getSystemPath() == null ? null
                        : dependency.getSystemPath(), null );
        iterateExclusion( innerCount, root, dependency.getExclusions(), "exclusions", "exclusion" );
        Utils.findAndReplaceSimpleElement( innerCount,
                                     root,
                                     "optional",
                                     dependency.getOptional() == null ? null : dependency.getOptional(),
                                     null );
    } // -- void updateDependency( Dependency, String, Counter, Element )

    /**
     * Method updateDependencyManagement.
     * 
     * @param dependencyManagement
     * @param element
     * @param counter
     * @param xmlTag
     */
    protected void updateDependencyManagement( final DependencyManagement dependencyManagement, final String xmlTag,
                                               final IndentationCounter counter, final Element element )
    {
        final boolean shouldExist = ( dependencyManagement != null );
        final Element root = Utils.updateElement( counter, element, xmlTag, shouldExist );
        if ( shouldExist )
        {
            final IndentationCounter innerCount = new IndentationCounter( counter.getDepth() + 1 );
            iterateDependency( innerCount, root, dependencyManagement.getDependencies(), "dependencies", "dependency" );
        }
    } // -- void updateDependencyManagement( DependencyManagement, String, Counter, Element )

    /**
     * Method updateDeploymentRepository.
     * 
     * @param deploymentRepository
     * @param element
     * @param counter
     * @param xmlTag
     */
    protected void updateDeploymentRepository( final DeploymentRepository deploymentRepository, final String xmlTag,
                                               final IndentationCounter counter, final Element element )
    {
        final boolean shouldExist = ( deploymentRepository != null );
        final Element root = Utils.updateElement( counter, element, xmlTag, shouldExist );
        if ( shouldExist )
        {
            final IndentationCounter innerCount = new IndentationCounter( counter.getDepth() + 1 );
            Utils.findAndReplaceSimpleElement( innerCount,
                                         root,
                                         "uniqueVersion",
                                         deploymentRepository.isUniqueVersion() == true ? null
                                                         : String.valueOf( deploymentRepository.isUniqueVersion() ),
                                         "true" );
            updateRepositoryPolicy( deploymentRepository.getReleases(), "releases", innerCount, root );
            updateRepositoryPolicy( deploymentRepository.getSnapshots(), "snapshots", innerCount, root );
            Utils.findAndReplaceSimpleElement( innerCount, root, "id", deploymentRepository.getId() == null ? null
                            : deploymentRepository.getId(), null );
            Utils.findAndReplaceSimpleElement( innerCount, root, "name", deploymentRepository.getName() == null ? null
                            : deploymentRepository.getName(), null );
            Utils.findAndReplaceSimpleElement( innerCount, root, "url", deploymentRepository.getUrl() == null ? null
                            : deploymentRepository.getUrl(), null );
            Utils.findAndReplaceSimpleElement( innerCount, root, "layout", deploymentRepository.getLayout() == null ? null
                            : deploymentRepository.getLayout(), "default" );
        }
    } // -- void updateDeploymentRepository( DeploymentRepository, String, Counter, Element )

    /**
     * Method updateDeveloper.
     * 
     * @param developer
     * @param element
     * @param counter
     * @param xmlTag
     */
    protected void updateDeveloper( final Developer developer, final String xmlTag, final IndentationCounter counter,
                                    final Element element )
    {
        final Element root = element;
        final IndentationCounter innerCount = new IndentationCounter( counter.getDepth() + 1 );
        Utils.findAndReplaceSimpleElement( innerCount, root, "id", developer.getId() == null ? null : developer.getId(), null );
        Utils.findAndReplaceSimpleElement( innerCount,
                                     root,
                                     "name",
                                     developer.getName() == null ? null : developer.getName(),
                                     null );
        Utils.findAndReplaceSimpleElement( innerCount,
                                     root,
                                     "email",
                                     developer.getEmail() == null ? null : developer.getEmail(),
                                     null );
        Utils.findAndReplaceSimpleElement( innerCount,
                                     root,
                                     "url",
                                     developer.getUrl() == null ? null : developer.getUrl(),
                                     null );
        Utils.findAndReplaceSimpleElement( innerCount, root, "organization", developer.getOrganization() == null ? null
                        : developer.getOrganization(), null );
        Utils.findAndReplaceSimpleElement( innerCount, root, "organizationUrl", developer.getOrganizationUrl() == null ? null
                        : developer.getOrganizationUrl(), null );
        Utils.findAndReplaceSimpleLists( innerCount, root, developer.getRoles(), "roles", "role" );
        Utils.findAndReplaceSimpleElement( innerCount,
                                     root,
                                     "timezone",
                                     developer.getTimezone() == null ? null : developer.getTimezone(),
                                     null );
        Utils.findAndReplaceProperties( innerCount, root, "properties", developer.getProperties() );
    } // -- void updateDeveloper( Developer, String, Counter, Element )

    /**
     * Method updateDistributionManagement.
     * 
     * @param distributionManagement
     * @param element
     * @param counter
     * @param xmlTag
     */
    protected void updateDistributionManagement( final DistributionManagement distributionManagement,
                                                 final String xmlTag, final IndentationCounter counter,
                                                 final Element element )
    {
        final boolean shouldExist = ( distributionManagement != null );
        final Element root = Utils.updateElement( counter, element, xmlTag, shouldExist );
        if ( shouldExist )
        {
            final IndentationCounter innerCount = new IndentationCounter( counter.getDepth() + 1 );
            updateDeploymentRepository( distributionManagement.getRepository(), "repository", innerCount, root );
            updateDeploymentRepository( distributionManagement.getSnapshotRepository(),
                                        "snapshotRepository",
                                        innerCount,
                                        root );
            updateSite( distributionManagement.getSite(), "site", innerCount, root );
            Utils.findAndReplaceSimpleElement( innerCount,
                                         root,
                                         "downloadUrl",
                                         distributionManagement.getDownloadUrl() == null ? null
                                                         : distributionManagement.getDownloadUrl(),
                                         null );
            updateRelocation( distributionManagement.getRelocation(), "relocation", innerCount, root );
            Utils.findAndReplaceSimpleElement( innerCount, root, "status", distributionManagement.getStatus() == null ? null
                            : distributionManagement.getStatus(), null );
        }
    } // -- void updateDistributionManagement( DistributionManagement, String, Counter, Element )

    /**
     * Method updateExclusion.
     * 
     * @param exclusion
     * @param element
     * @param counter
     * @param xmlTag
     */
    protected void updateExclusion( final Exclusion exclusion, final String xmlTag, final IndentationCounter counter,
                                    final Element element )
    {
        final Element root = element;
        final IndentationCounter innerCount = new IndentationCounter( counter.getDepth() + 1 );
        Utils.findAndReplaceSimpleElement( innerCount, root, "artifactId", exclusion.getArtifactId() == null ? null
                        : exclusion.getArtifactId(), null );
        Utils.findAndReplaceSimpleElement( innerCount,
                                     root,
                                     "groupId",
                                     exclusion.getGroupId() == null ? null : exclusion.getGroupId(),
                                     null );
    } // -- void updateExclusion( Exclusion, String, Counter, Element )

    /**
     * Method updateExtension.
     * 
     * @param extension
     * @param element
     * @param counter
     * @param xmlTag
     */
    protected void updateExtension( final Extension extension, final String xmlTag, final IndentationCounter counter,
                                    final Element element )
    {
        final Element root = element;
        final IndentationCounter innerCount = new IndentationCounter( counter.getDepth() + 1 );
        Utils.findAndReplaceSimpleElement( innerCount,
                                     root,
                                     "groupId",
                                     extension.getGroupId() == null ? null : extension.getGroupId(),
                                     null );
        Utils.findAndReplaceSimpleElement( innerCount, root, "artifactId", extension.getArtifactId() == null ? null
                        : extension.getArtifactId(), null );
        Utils.findAndReplaceSimpleElement( innerCount,
                                     root,
                                     "version",
                                     extension.getVersion() == null ? null : extension.getVersion(),
                                     null );
    } // -- void updateExtension( Extension, String, Counter, Element )

    /**
     * Method updateFileSet.
     * 
     * @param fileSet
     * @param element
     * @param counter
     * @param xmlTag
     */
    protected void updateFileSet( final FileSet fileSet, final String xmlTag, final IndentationCounter counter,
                                  final Element element )
    {
        final boolean shouldExist = ( fileSet != null );
        final Element root = Utils.updateElement( counter, element, xmlTag, shouldExist );
        if ( shouldExist )
        {
            final IndentationCounter innerCount = new IndentationCounter( counter.getDepth() + 1 );
            Utils.findAndReplaceSimpleElement( innerCount,
                                         root,
                                         "directory",
                                         fileSet.getDirectory() == null ? null : fileSet.getDirectory(),
                                         null );
            Utils.findAndReplaceSimpleLists( innerCount, root, fileSet.getIncludes(), "includes", "include" );
            Utils.findAndReplaceSimpleLists( innerCount, root, fileSet.getExcludes(), "excludes", "exclude" );
        }
    } // -- void updateFileSet( FileSet, String, Counter, Element )

    /**
     * Method updateIssueManagement.
     * 
     * @param issueManagement
     * @param element
     * @param counter
     * @param xmlTag
     */
    protected void updateIssueManagement( final IssueManagement issueManagement, final String xmlTag,
                                          final IndentationCounter counter, final Element element )
    {
        final boolean shouldExist = ( issueManagement != null );
        final Element root = Utils.updateElement( counter, element, xmlTag, shouldExist );
        if ( shouldExist )
        {
            final IndentationCounter innerCount = new IndentationCounter( counter.getDepth() + 1 );
            Utils.findAndReplaceSimpleElement( innerCount, root, "system", issueManagement.getSystem() == null ? null
                            : issueManagement.getSystem(), null );
            Utils.findAndReplaceSimpleElement( innerCount, root, "url", issueManagement.getUrl() == null ? null
                            : issueManagement.getUrl(), null );
        }
    } // -- void updateIssueManagement( IssueManagement, String, Counter, Element )

    /**
     * Method updateLicense.
     * 
     * @param license
     * @param element
     * @param counter
     * @param xmlTag
     */
    protected void updateLicense( final License license, final String xmlTag, final IndentationCounter counter,
                                  final Element element )
    {
        final Element root = element;
        final IndentationCounter innerCount = new IndentationCounter( counter.getDepth() + 1 );
        Utils.findAndReplaceSimpleElement( innerCount,
                                     root,
                                     "name",
                                     license.getName() == null ? null : license.getName(),
                                     null );
        Utils.findAndReplaceSimpleElement( innerCount, root, "url", license.getUrl() == null ? null : license.getUrl(), null );
        Utils.findAndReplaceSimpleElement( innerCount, root, "distribution", license.getDistribution() == null ? null
                        : license.getDistribution(), null );
        Utils.findAndReplaceSimpleElement( innerCount,
                                     root,
                                     "comments",
                                     license.getComments() == null ? null : license.getComments(),
                                     null );
    } // -- void updateLicense( License, String, Counter, Element )

    /**
     * Method updateMailingList.
     * 
     * @param mailingList
     * @param element
     * @param counter
     * @param xmlTag
     */
    protected void updateMailingList( final MailingList mailingList, final String xmlTag,
                                      final IndentationCounter counter, final Element element )
    {
        final Element root = element;
        final IndentationCounter innerCount = new IndentationCounter( counter.getDepth() + 1 );
        Utils.findAndReplaceSimpleElement( innerCount,
                                     root,
                                     "name",
                                     mailingList.getName() == null ? null : mailingList.getName(),
                                     null );
        Utils.findAndReplaceSimpleElement( innerCount, root, "subscribe", mailingList.getSubscribe() == null ? null
                        : mailingList.getSubscribe(), null );
        Utils.findAndReplaceSimpleElement( innerCount, root, "unsubscribe", mailingList.getUnsubscribe() == null ? null
                        : mailingList.getUnsubscribe(), null );
        Utils.findAndReplaceSimpleElement( innerCount,
                                     root,
                                     "post",
                                     mailingList.getPost() == null ? null : mailingList.getPost(),
                                     null );
        Utils.findAndReplaceSimpleElement( innerCount,
                                     root,
                                     "archive",
                                     mailingList.getArchive() == null ? null : mailingList.getArchive(),
                                     null );
        Utils.findAndReplaceSimpleLists( innerCount, root, mailingList.getOtherArchives(), "otherArchives", "otherArchive" );
    } // -- void updateMailingList( MailingList, String, Counter, Element )

    /**
     * Method updateModel.
     * 
     * @param model
     * @param element
     * @param counter
     * @param xmlTag
     */
    protected void updateModel( final Model model, final String xmlTag, final IndentationCounter counter,
                                final Element element )
    {
        final Element root = element;
        final IndentationCounter innerCount = new IndentationCounter( counter.getDepth() + 1 );
        Utils.findAndReplaceSimpleElement( innerCount,
                                     root,
                                     "modelVersion",
                                     model.getModelVersion() == null ? null : model.getModelVersion(),
                                     null );
        updateParent( model.getParent(), "parent", innerCount, root );
        Utils.findAndReplaceSimpleElement( innerCount,
                                     root,
                                     "groupId",
                                     model.getGroupId() == null ? null : model.getGroupId(),
                                     null );
        Utils.findAndReplaceSimpleElement( innerCount,
                                     root,
                                     "artifactId",
                                     model.getArtifactId() == null ? null : model.getArtifactId(),
                                     null );
        Utils.findAndReplaceSimpleElement( innerCount,
                                     root,
                                     "version",
                                     model.getVersion() == null ? null : model.getVersion(),
                                     null );
        Utils.findAndReplaceSimpleElement( innerCount,
                                     root,
                                     "packaging",
                                     model.getPackaging() == null ? null : model.getPackaging(),
                                     "jar" );
        Utils.findAndReplaceSimpleElement( innerCount, root, "name", model.getName() == null ? null : model.getName(), null );
        Utils.findAndReplaceSimpleElement( innerCount,
                                     root,
                                     "description",
                                     model.getDescription() == null ? null : model.getDescription(),
                                     null );
        Utils.findAndReplaceSimpleElement( innerCount, root, "url", model.getUrl() == null ? null : model.getUrl(), null );
        Utils.findAndReplaceSimpleElement( innerCount,
                                     root,
                                     "inceptionYear",
                                     model.getInceptionYear() == null ? null : model.getInceptionYear(),
                                     null );
        updateOrganization( model.getOrganization(), "organization", innerCount, root );
        iterateLicense( innerCount, root, model.getLicenses(), "licenses", "license" );
        iterateDeveloper( innerCount, root, model.getDevelopers(), "developers", "developer" );
        iterateContributor( innerCount, root, model.getContributors(), "contributors", "contributor" );
        iterateMailingList( innerCount, root, model.getMailingLists(), "mailingLists", "mailingList" );
        updatePrerequisites( model.getPrerequisites(), "prerequisites", innerCount, root );
        Utils.findAndReplaceSimpleLists( innerCount, root, model.getModules(), "modules", "module" );
        updateScm( model.getScm(), "scm", innerCount, root );
        updateIssueManagement( model.getIssueManagement(), "issueManagement", innerCount, root );
        updateCiManagement( model.getCiManagement(), "ciManagement", innerCount, root );
        updateDistributionManagement( model.getDistributionManagement(), "distributionManagement", innerCount, root );
        Utils.findAndReplaceProperties( innerCount, root, "properties", model.getProperties() );
        updateDependencyManagement( model.getDependencyManagement(), "dependencyManagement", innerCount, root );
        iterateDependency( innerCount, root, model.getDependencies(), "dependencies", "dependency" );
        iterateRepository( innerCount, root, model.getRepositories(), "repositories", "repository" );
        iterateRepository( innerCount, root, model.getPluginRepositories(), "pluginRepositories", "pluginRepository" );
        updateBuild( model.getBuild(), "build", innerCount, root );
        Utils.findAndReplaceXpp3DOM( innerCount, root, "reports", (Xpp3Dom) model.getReports() );
        updateReporting( model.getReporting(), "reporting", innerCount, root );
        iterateProfile( innerCount, root, model.getProfiles(), "profiles", "profile" );
    } // -- void updateModel( Model, String, Counter, Element )

    /**
     * Method updateModelBase.
     * 
     * @param modelBase
     * @param element
     * @param counter
     * @param xmlTag
     */
    protected void updateModelBase( final ModelBase modelBase, final String xmlTag, final IndentationCounter counter,
                                    final Element element )
    {
        final boolean shouldExist = ( modelBase != null );
        final Element root = Utils.updateElement( counter, element, xmlTag, shouldExist );
        if ( shouldExist )
        {
            final IndentationCounter innerCount = new IndentationCounter( counter.getDepth() + 1 );
            Utils.findAndReplaceSimpleLists( innerCount, root, modelBase.getModules(), "modules", "module" );
            updateDistributionManagement( modelBase.getDistributionManagement(),
                                          "distributionManagement",
                                          innerCount,
                                          root );
            Utils.findAndReplaceProperties( innerCount, root, "properties", modelBase.getProperties() );
            updateDependencyManagement( modelBase.getDependencyManagement(), "dependencyManagement", innerCount, root );
            iterateDependency( innerCount, root, modelBase.getDependencies(), "dependencies", "dependency" );
            iterateRepository( innerCount, root, modelBase.getRepositories(), "repositories", "repository" );
            iterateRepository( innerCount,
                               root,
                               modelBase.getPluginRepositories(),
                               "pluginRepositories",
                               "pluginRepository" );
            Utils.findAndReplaceXpp3DOM( innerCount, root, "reports", (Xpp3Dom) modelBase.getReports() );
            updateReporting( modelBase.getReporting(), "reporting", innerCount, root );
        }
    } // -- void updateModelBase( ModelBase, String, Counter, Element )

    /**
     * Method updateNotifier.
     * 
     * @param notifier
     * @param element
     * @param counter
     * @param xmlTag
     */
    protected void updateNotifier( final Notifier notifier, final String xmlTag, final IndentationCounter counter,
                                   final Element element )
    {
        final Element root = element;
        final IndentationCounter innerCount = new IndentationCounter( counter.getDepth() + 1 );
        Utils.findAndReplaceSimpleElement( innerCount,
                                     root,
                                     "type",
                                     notifier.getType() == null ? null : notifier.getType(),
                                     "mail" );
        Utils.findAndReplaceSimpleElement( innerCount,
                                     root,
                                     "sendOnError",
                                     notifier.isSendOnError() == true ? null
                                                     : String.valueOf( notifier.isSendOnError() ),
                                     "true" );
        Utils.findAndReplaceSimpleElement( innerCount, root, "sendOnFailure", notifier.isSendOnFailure() == true ? null
                        : String.valueOf( notifier.isSendOnFailure() ), "true" );
        Utils.findAndReplaceSimpleElement( innerCount, root, "sendOnSuccess", notifier.isSendOnSuccess() == true ? null
                        : String.valueOf( notifier.isSendOnSuccess() ), "true" );
        Utils.findAndReplaceSimpleElement( innerCount, root, "sendOnWarning", notifier.isSendOnWarning() == true ? null
                        : String.valueOf( notifier.isSendOnWarning() ), "true" );
        Utils.findAndReplaceSimpleElement( innerCount,
                                     root,
                                     "address",
                                     notifier.getAddress() == null ? null : notifier.getAddress(),
                                     null );
        Utils.findAndReplaceProperties( innerCount, root, "configuration", notifier.getConfiguration() );
    } // -- void updateNotifier( Notifier, String, Counter, Element )

    /**
     * Method updateOrganization.
     * 
     * @param organization
     * @param element
     * @param counter
     * @param xmlTag
     */
    protected void updateOrganization( final Organization organization, final String xmlTag,
                                       final IndentationCounter counter, final Element element )
    {
        final boolean shouldExist = ( organization != null );
        final Element root = Utils.updateElement( counter, element, xmlTag, shouldExist );
        if ( shouldExist )
        {
            final IndentationCounter innerCount = new IndentationCounter( counter.getDepth() + 1 );
            Utils.findAndReplaceSimpleElement( innerCount,
                                         root,
                                         "name",
                                         organization.getName() == null ? null : organization.getName(),
                                         null );
            Utils.findAndReplaceSimpleElement( innerCount,
                                         root,
                                         "url",
                                         organization.getUrl() == null ? null : organization.getUrl(),
                                         null );
        }
    } // -- void updateOrganization( Organization, String, Counter, Element )

    /**
     * Method updateParent.
     * 
     * @param parent
     * @param element
     * @param counter
     * @param xmlTag
     */
    protected void updateParent( final Parent parent, final String xmlTag, final IndentationCounter counter,
                                 final Element element )
    {
        final boolean shouldExist = ( parent != null );
        final Element root = Utils.updateElement( counter, element, xmlTag, shouldExist );
        if ( shouldExist )
        {
            final IndentationCounter innerCount = new IndentationCounter( counter.getDepth() + 1 );
            Utils.findAndReplaceSimpleElement( innerCount,
                                         root,
                                         "artifactId",
                                         parent.getArtifactId() == null ? null : parent.getArtifactId(),
                                         null );
            Utils.findAndReplaceSimpleElement( innerCount,
                                         root,
                                         "groupId",
                                         parent.getGroupId() == null ? null : parent.getGroupId(),
                                         null );
            Utils.findAndReplaceSimpleElement( innerCount,
                                         root,
                                         "version",
                                         parent.getVersion() == null ? null : parent.getVersion(),
                                         null );
            Utils.findAndReplaceSimpleElement( innerCount, root, "relativePath", parent.getRelativePath() == null ? null
                            : parent.getRelativePath(), "../pom.xml" );
        }
    } // -- void updateParent( Parent, String, Counter, Element )

    /**
     * Method updatePlugin.
     * 
     * @param plugin
     * @param element
     * @param counter
     * @param xmlTag
     */
    protected void updatePlugin( final Plugin plugin, final String xmlTag, final IndentationCounter counter,
                                 final Element element )
    {
        final Element root = element;
        final IndentationCounter innerCount = new IndentationCounter( counter.getDepth() + 1 );
        Utils.findAndReplaceSimpleElement( innerCount,
                                     root,
                                     "groupId",
                                     plugin.getGroupId() == null ? null : plugin.getGroupId(),
                                     "org.apache.maven.plugins" );
        Utils.findAndReplaceSimpleElement( innerCount,
                                     root,
                                     "artifactId",
                                     plugin.getArtifactId() == null ? null : plugin.getArtifactId(),
                                     null );
        Utils.findAndReplaceSimpleElement( innerCount,
                                     root,
                                     "version",
                                     plugin.getVersion() == null ? null : plugin.getVersion(),
                                     null );
        Utils.findAndReplaceSimpleElement( innerCount,
                                     root,
                                     "extensions",
                                     plugin.getExtensions() == null ? null : plugin.getExtensions(),
                                     null );
        iteratePluginExecution( innerCount, root, plugin.getExecutions(), "executions", "execution" );
        iterateDependency( innerCount, root, plugin.getDependencies(), "dependencies", "dependency" );
        Utils.findAndReplaceXpp3DOM( innerCount, root, "goals", (Xpp3Dom) plugin.getGoals() );
        Utils.findAndReplaceSimpleElement( innerCount,
                                     root,
                                     "inherited",
                                     plugin.getInherited() == null ? null : plugin.getInherited(),
                                     null );
        Utils.findAndReplaceXpp3DOM( innerCount, root, "configuration", (Xpp3Dom) plugin.getConfiguration() );
    } // -- void updatePlugin( Plugin, String, Counter, Element )

    /**
     * Method updatePluginConfiguration.
     * 
     * @param pluginConfiguration
     * @param element
     * @param counter
     * @param xmlTag
     */
    protected void updatePluginConfiguration( final PluginConfiguration pluginConfiguration, final String xmlTag,
                                              final IndentationCounter counter, final Element element )
    {
        final boolean shouldExist = ( pluginConfiguration != null );
        final Element root = Utils.updateElement( counter, element, xmlTag, shouldExist );
        if ( shouldExist )
        {
            final IndentationCounter innerCount = new IndentationCounter( counter.getDepth() + 1 );
            updatePluginManagement( pluginConfiguration.getPluginManagement(), "pluginManagement", innerCount, root );
            iteratePlugin( innerCount, root, pluginConfiguration.getPlugins(), "plugins", "plugin" );
        }
    } // -- void updatePluginConfiguration( PluginConfiguration, String, Counter, Element )

    /**
     * Method updatePluginContainer.
     * 
     * @param pluginContainer
     * @param element
     * @param counter
     * @param xmlTag
     */
    protected void updatePluginContainer( final PluginContainer pluginContainer, final String xmlTag,
                                          final IndentationCounter counter, final Element element )
    {
        final boolean shouldExist = ( pluginContainer != null );
        final Element root = Utils.updateElement( counter, element, xmlTag, shouldExist );
        if ( shouldExist )
        {
            final IndentationCounter innerCount = new IndentationCounter( counter.getDepth() + 1 );
            iteratePlugin( innerCount, root, pluginContainer.getPlugins(), "plugins", "plugin" );
        }
    } // -- void updatePluginContainer( PluginContainer, String, Counter, Element )

    /**
     * Method updatePluginExecution.
     * 
     * @param pluginExecution
     * @param element
     * @param counter
     * @param xmlTag
     */
    protected void updatePluginExecution( final PluginExecution pluginExecution, final String xmlTag,
                                          final IndentationCounter counter, final Element element )
    {
        final Element root = element;
        final IndentationCounter innerCount = new IndentationCounter( counter.getDepth() + 1 );
        Utils.findAndReplaceSimpleElement( innerCount,
                                     root,
                                     "id",
                                     pluginExecution.getId() == null ? null : pluginExecution.getId(),
                                     "default" );
        Utils.findAndReplaceSimpleElement( innerCount, root, "phase", pluginExecution.getPhase() == null ? null
                        : pluginExecution.getPhase(), null );
        Utils.findAndReplaceSimpleLists( innerCount, root, pluginExecution.getGoals(), "goals", "goal" );
        Utils.findAndReplaceSimpleElement( innerCount, root, "inherited", pluginExecution.getInherited() == null ? null
                        : pluginExecution.getInherited(), null );
        Utils.findAndReplaceXpp3DOM( innerCount, root, "configuration", (Xpp3Dom) pluginExecution.getConfiguration() );
    } // -- void updatePluginExecution( PluginExecution, String, Counter, Element )

    /**
     * Method updatePluginManagement.
     * 
     * @param pluginManagement
     * @param element
     * @param counter
     * @param xmlTag
     */
    protected void updatePluginManagement( final PluginManagement pluginManagement, final String xmlTag,
                                           final IndentationCounter counter, final Element element )
    {
        final boolean shouldExist = ( pluginManagement != null );
        final Element root = Utils.updateElement( counter, element, xmlTag, shouldExist );
        if ( shouldExist )
        {
            final IndentationCounter innerCount = new IndentationCounter( counter.getDepth() + 1 );
            iteratePlugin( innerCount, root, pluginManagement.getPlugins(), "plugins", "plugin" );
        }
    } // -- void updatePluginManagement( PluginManagement, String, Counter, Element )

    /**
     * Method updatePrerequisites.
     * 
     * @param prerequisites
     * @param element
     * @param counter
     * @param xmlTag
     */
    protected void updatePrerequisites( final Prerequisites prerequisites, final String xmlTag,
                                        final IndentationCounter counter, final Element element )
    {
        final boolean shouldExist = ( prerequisites != null );
        final Element root = Utils.updateElement( counter, element, xmlTag, shouldExist );
        if ( shouldExist )
        {
            final IndentationCounter innerCount = new IndentationCounter( counter.getDepth() + 1 );
            Utils.findAndReplaceSimpleElement( innerCount, root, "maven", prerequisites.getMaven() == null ? null
                            : prerequisites.getMaven(), "2.0" );
        }
    } // -- void updatePrerequisites( Prerequisites, String, Counter, Element )

    /**
     * Method updateProfile.
     * 
     * @param profile
     * @param element
     * @param counter
     * @param xmlTag
     */
    protected void updateProfile( final Profile profile, final String xmlTag, final IndentationCounter counter,
                                  final Element element )
    {
        final Element root = element;
        final IndentationCounter innerCount = new IndentationCounter( counter.getDepth() + 1 );
        Utils.findAndReplaceSimpleElement( innerCount,
                                     root,
                                     "id",
                                     profile.getId() == null ? null : profile.getId(),
                                     "default" );
        updateActivation( profile.getActivation(), "activation", innerCount, root );
        updateBuildBase( profile.getBuild(), "build", innerCount, root );
        Utils.findAndReplaceSimpleLists( innerCount, root, profile.getModules(), "modules", "module" );
        updateDistributionManagement( profile.getDistributionManagement(), "distributionManagement", innerCount, root );
        Utils.findAndReplaceProperties( innerCount, root, "properties", profile.getProperties() );
        updateDependencyManagement( profile.getDependencyManagement(), "dependencyManagement", innerCount, root );
        iterateDependency( innerCount, root, profile.getDependencies(), "dependencies", "dependency" );
        iterateRepository( innerCount, root, profile.getRepositories(), "repositories", "repository" );
        iterateRepository( innerCount, root, profile.getPluginRepositories(), "pluginRepositories", "pluginRepository" );
        Utils.findAndReplaceXpp3DOM( innerCount, root, "reports", (Xpp3Dom) profile.getReports() );
        updateReporting( profile.getReporting(), "reporting", innerCount, root );
    } // -- void updateProfile( Profile, String, Counter, Element )

    /**
     * Method updateRelocation.
     * 
     * @param relocation
     * @param element
     * @param counter
     * @param xmlTag
     */
    protected void updateRelocation( final Relocation relocation, final String xmlTag,
                                     final IndentationCounter counter, final Element element )
    {
        final boolean shouldExist = ( relocation != null );
        final Element root = Utils.updateElement( counter, element, xmlTag, shouldExist );
        if ( shouldExist )
        {
            final IndentationCounter innerCount = new IndentationCounter( counter.getDepth() + 1 );
            Utils.findAndReplaceSimpleElement( innerCount, root, "groupId", relocation.getGroupId() == null ? null
                            : relocation.getGroupId(), null );
            Utils.findAndReplaceSimpleElement( innerCount, root, "artifactId", relocation.getArtifactId() == null ? null
                            : relocation.getArtifactId(), null );
            Utils.findAndReplaceSimpleElement( innerCount, root, "version", relocation.getVersion() == null ? null
                            : relocation.getVersion(), null );
            Utils.findAndReplaceSimpleElement( innerCount, root, "message", relocation.getMessage() == null ? null
                            : relocation.getMessage(), null );
        }
    } // -- void updateRelocation( Relocation, String, Counter, Element )

    /**
     * Method updateReportPlugin.
     * 
     * @param reportPlugin
     * @param element
     * @param counter
     * @param xmlTag
     */
    protected void updateReportPlugin( final ReportPlugin reportPlugin, final String xmlTag,
                                       final IndentationCounter counter, final Element element )
    {
        final Element root = element;
        final IndentationCounter innerCount = new IndentationCounter( counter.getDepth() + 1 );
        Utils.findAndReplaceSimpleElement( innerCount, root, "groupId", reportPlugin.getGroupId() == null ? null
                        : reportPlugin.getGroupId(), "org.apache.maven.plugins" );
        Utils.findAndReplaceSimpleElement( innerCount, root, "artifactId", reportPlugin.getArtifactId() == null ? null
                        : reportPlugin.getArtifactId(), null );
        Utils.findAndReplaceSimpleElement( innerCount, root, "version", reportPlugin.getVersion() == null ? null
                        : reportPlugin.getVersion(), null );
        iterateReportSet( innerCount, root, reportPlugin.getReportSets(), "reportSets", "reportSet" );
        Utils.findAndReplaceSimpleElement( innerCount, root, "inherited", reportPlugin.getInherited() == null ? null
                        : reportPlugin.getInherited(), null );
        Utils.findAndReplaceXpp3DOM( innerCount, root, "configuration", (Xpp3Dom) reportPlugin.getConfiguration() );
    } // -- void updateReportPlugin( ReportPlugin, String, Counter, Element )

    /**
     * Method updateReportSet.
     * 
     * @param reportSet
     * @param element
     * @param counter
     * @param xmlTag
     */
    protected void updateReportSet( final ReportSet reportSet, final String xmlTag, final IndentationCounter counter,
                                    final Element element )
    {
        final Element root = element;
        final IndentationCounter innerCount = new IndentationCounter( counter.getDepth() + 1 );
        Utils.findAndReplaceSimpleElement( innerCount,
                                     root,
                                     "id",
                                     reportSet.getId() == null ? null : reportSet.getId(),
                                     "default" );
        Utils.findAndReplaceSimpleLists( innerCount, root, reportSet.getReports(), "reports", "report" );
        Utils.findAndReplaceSimpleElement( innerCount,
                                     root,
                                     "inherited",
                                     reportSet.getInherited() == null ? null : reportSet.getInherited(),
                                     null );
        Utils.findAndReplaceXpp3DOM( innerCount, root, "configuration", (Xpp3Dom) reportSet.getConfiguration() );
    } // -- void updateReportSet( ReportSet, String, Counter, Element )

    /**
     * Method updateReporting.
     * 
     * @param reporting
     * @param element
     * @param counter
     * @param xmlTag
     */
    protected void updateReporting( final Reporting reporting, final String xmlTag, final IndentationCounter counter,
                                    final Element element )
    {
        final boolean shouldExist = ( reporting != null );
        final Element root = Utils.updateElement( counter, element, xmlTag, shouldExist );
        if ( shouldExist )
        {
            final IndentationCounter innerCount = new IndentationCounter( counter.getDepth() + 1 );
            Utils.findAndReplaceSimpleElement( innerCount,
                                         root,
                                         "excludeDefaults",
                                         reporting.getExcludeDefaults() == null ? null : reporting.getExcludeDefaults(),
                                         null );
            Utils.findAndReplaceSimpleElement( innerCount,
                                         root,
                                         "outputDirectory",
                                         reporting.getOutputDirectory() == null ? null : reporting.getOutputDirectory(),
                                         null );
            iterateReportPlugin( innerCount, root, reporting.getPlugins(), "plugins", "plugin" );
        }
    } // -- void updateReporting( Reporting, String, Counter, Element )

    /**
     * Method updateRepository.
     * 
     * @param repository
     * @param element
     * @param counter
     * @param xmlTag
     */
    protected void updateRepository( final Repository repository, final String xmlTag,
                                     final IndentationCounter counter, final Element element )
    {
        final Element root = element;
        final IndentationCounter innerCount = new IndentationCounter( counter.getDepth() + 1 );
        updateRepositoryPolicy( repository.getReleases(), "releases", innerCount, root );
        updateRepositoryPolicy( repository.getSnapshots(), "snapshots", innerCount, root );
        Utils.findAndReplaceSimpleElement( innerCount,
                                     root,
                                     "id",
                                     repository.getId() == null ? null : repository.getId(),
                                     null );
        Utils.findAndReplaceSimpleElement( innerCount,
                                     root,
                                     "name",
                                     repository.getName() == null ? null : repository.getName(),
                                     null );
        Utils.findAndReplaceSimpleElement( innerCount,
                                     root,
                                     "url",
                                     repository.getUrl() == null ? null : repository.getUrl(),
                                     null );
        Utils.findAndReplaceSimpleElement( innerCount,
                                     root,
                                     "layout",
                                     repository.getLayout() == null ? null : repository.getLayout(),
                                     "default" );
    } // -- void updateRepository( Repository, String, Counter, Element )

    /**
     * Method updateRepositoryBase.
     * 
     * @param repositoryBase
     * @param element
     * @param counter
     * @param xmlTag
     */
    protected void updateRepositoryBase( final RepositoryBase repositoryBase, final String xmlTag,
                                         final IndentationCounter counter, final Element element )
    {
        final boolean shouldExist = ( repositoryBase != null );
        final Element root = Utils.updateElement( counter, element, xmlTag, shouldExist );
        if ( shouldExist )
        {
            final IndentationCounter innerCount = new IndentationCounter( counter.getDepth() + 1 );
            Utils.findAndReplaceSimpleElement( innerCount,
                                         root,
                                         "id",
                                         repositoryBase.getId() == null ? null : repositoryBase.getId(),
                                         null );
            Utils.findAndReplaceSimpleElement( innerCount, root, "name", repositoryBase.getName() == null ? null
                            : repositoryBase.getName(), null );
            Utils.findAndReplaceSimpleElement( innerCount, root, "url", repositoryBase.getUrl() == null ? null
                            : repositoryBase.getUrl(), null );
            Utils.findAndReplaceSimpleElement( innerCount, root, "layout", repositoryBase.getLayout() == null ? null
                            : repositoryBase.getLayout(), "default" );
        }
    } // -- void updateRepositoryBase( RepositoryBase, String, Counter, Element )

    /**
     * Method updateRepositoryPolicy.
     * 
     * @param repositoryPolicy
     * @param element
     * @param counter
     * @param xmlTag
     */
    protected void updateRepositoryPolicy( final RepositoryPolicy repositoryPolicy, final String xmlTag,
                                           final IndentationCounter counter, final Element element )
    {
        final boolean shouldExist = ( repositoryPolicy != null );
        final Element root = Utils.updateElement( counter, element, xmlTag, shouldExist );
        if ( shouldExist )
        {
            final IndentationCounter innerCount = new IndentationCounter( counter.getDepth() + 1 );
            Utils.findAndReplaceSimpleElement( innerCount, root, "enabled", repositoryPolicy.getEnabled() == null ? null
                            : repositoryPolicy.getEnabled(), null );
            Utils.findAndReplaceSimpleElement( innerCount,
                                         root,
                                         "updatePolicy",
                                         repositoryPolicy.getUpdatePolicy() == null ? null
                                                         : repositoryPolicy.getUpdatePolicy(),
                                         null );
            Utils.findAndReplaceSimpleElement( innerCount,
                                         root,
                                         "checksumPolicy",
                                         repositoryPolicy.getChecksumPolicy() == null ? null
                                                         : repositoryPolicy.getChecksumPolicy(),
                                         null );
        }
    } // -- void updateRepositoryPolicy( RepositoryPolicy, String, Counter, Element )

    /**
     * Method updateResource.
     * 
     * @param resource
     * @param element
     * @param counter
     * @param xmlTag
     */
    protected void updateResource( final Resource resource, final String xmlTag, final IndentationCounter counter,
                                   final Element element )
    {
        final Element root = element;
        final IndentationCounter innerCount = new IndentationCounter( counter.getDepth() + 1 );
        Utils.findAndReplaceSimpleElement( innerCount,
                                     root,
                                     "targetPath",
                                     resource.getTargetPath() == null ? null : resource.getTargetPath(),
                                     null );
        Utils.findAndReplaceSimpleElement( innerCount,
                                     root,
                                     "filtering",
                                     resource.getFiltering() == null ? null : resource.getFiltering(),
                                     null );
        Utils.findAndReplaceSimpleElement( innerCount,
                                     root,
                                     "directory",
                                     resource.getDirectory() == null ? null : resource.getDirectory(),
                                     null );
        Utils.findAndReplaceSimpleLists( innerCount, root, resource.getIncludes(), "includes", "include" );
        Utils.findAndReplaceSimpleLists( innerCount, root, resource.getExcludes(), "excludes", "exclude" );
    } // -- void updateResource( Resource, String, Counter, Element )

    /**
     * Method updateScm.
     * 
     * @param scm
     * @param element
     * @param counter
     * @param xmlTag
     */
    protected void updateScm( final Scm scm, final String xmlTag, final IndentationCounter counter,
                              final Element element )
    {
        final boolean shouldExist = ( scm != null );
        final Element root = Utils.updateElement( counter, element, xmlTag, shouldExist );
        if ( shouldExist )
        {
            final IndentationCounter innerCount = new IndentationCounter( counter.getDepth() + 1 );
            Utils.findAndReplaceSimpleElement( innerCount,
                                         root,
                                         "connection",
                                         scm.getConnection() == null ? null : scm.getConnection(),
                                         null );
            Utils.findAndReplaceSimpleElement( innerCount,
                                         root,
                                         "developerConnection",
                                         scm.getDeveloperConnection() == null ? null : scm.getDeveloperConnection(),
                                         null );
            Utils.findAndReplaceSimpleElement( innerCount, root, "tag", scm.getTag() == null ? null : scm.getTag(), "HEAD" );
            Utils.findAndReplaceSimpleElement( innerCount, root, "url", scm.getUrl() == null ? null : scm.getUrl(), null );
        }
    } // -- void updateScm( Scm, String, Counter, Element )

    /**
     * Method updateSite.
     * 
     * @param site
     * @param element
     * @param counter
     * @param xmlTag
     */
    protected void updateSite( final Site site, final String xmlTag, final IndentationCounter counter,
                               final Element element )
    {
        final boolean shouldExist = ( site != null );
        final Element root = Utils.updateElement( counter, element, xmlTag, shouldExist );
        if ( shouldExist )
        {
            final IndentationCounter innerCount = new IndentationCounter( counter.getDepth() + 1 );
            Utils.findAndReplaceSimpleElement( innerCount, root, "id", site.getId() == null ? null : site.getId(), null );
            Utils.findAndReplaceSimpleElement( innerCount, root, "name", site.getName() == null ? null : site.getName(), null );
            Utils.findAndReplaceSimpleElement( innerCount, root, "url", site.getUrl() == null ? null : site.getUrl(), null );
        }
    } // -- void updateSite( Site, String, Counter, Element )

    @Override
    protected void update( final Model source, final IndentationCounter indentationCounter, final Element rootElement )
        throws IOException
    {
        updateModel( source, "project", indentationCounter, rootElement );
    }

}
