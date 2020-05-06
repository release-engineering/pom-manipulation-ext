/*
 * Copyright (C) 2012 Red Hat, Inc.
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
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMFactory;
import org.jdom.UncheckedJDOMFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

@SuppressWarnings( { "rawtypes", "JavaDoc" } )
public class JDOMModelConverter
{
    protected final JDOMFactory factory = new UncheckedJDOMFactory();

    protected final Logger logger = LoggerFactory.getLogger( JDOMModelConverter.class );

    public JDOMModelConverter()
    {
    }

    /**
     * Writes the model to the document
     * @param model containing changes to propagate to the model.
     * @param document the document to write changes to.
     * @throws IOException if an error occurs.
     */
    public void convertModelToJDOM ( final Model model, Document document ) throws IOException
    {
        update( model, new IndentationCounter( 0 ), document.getRootElement() );
    }

    /**
     * Method iterateContributor.
     * @param counter
     * @param parent
     * @param list
     */
    protected void iterateContributor( final IndentationCounter counter, final Element parent, final Collection list )
    {
        final boolean shouldExist = ( list != null ) && ( list.size() > 0 );
        Element element = parent.getChild( "contributors", parent.getNamespace() );
        if ( element == null ) // If the list element already exists ignore it.
        {
            element = Utils.updateElement( counter, parent, "contributors", shouldExist );
        }
        if ( shouldExist )
        {
            final Iterator it = list.iterator();
            Iterator elIt = element.getChildren( "contributor", element.getNamespace() ).iterator();
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
                    el = factory.element( "contributor", element.getNamespace() );
                    Utils.insertAtPreferredLocation( element, el, innerCount );
                }
                updateContributor( value, "contributor", innerCount, el );
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
        else
        {
            removeExisting( element, "contributor" );
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
                                      final Collection list, final String parentTag,
                                      final String childTag )
    {
        final boolean shouldExist = ( list != null ) && ( list.size() > 0 );
        Element element = parent.getChild( parentTag, parent.getNamespace() );
        if ( element == null ) // If the list element already exists ignore it.
        {
            element = Utils.updateElement( counter, parent, parentTag, shouldExist );
        }
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
        else
        {
            removeExisting( element, childTag );
        }
    } // -- void iterateDependency( Counter, Element, java.util.Collection, java.lang.String, java.lang.String )

    /**
     * Method iterateDeveloper.
     * @param counter
     * @param parent
     * @param list
     */
    protected void iterateDeveloper( final IndentationCounter counter, final Element parent, final Collection list )
    {
        final boolean shouldExist = list != null && list.size() > 0;

        Element element = parent.getChild( "developers", parent.getNamespace() );
        if ( element == null ) // If the list element already exists ignore it.
        {
            element = Utils.updateElement( counter, parent, "developers", shouldExist );
        }
        if ( shouldExist )
        {
            final Iterator it = list.iterator();
            Iterator elIt = element.getChildren( "developer", element.getNamespace() ).iterator();
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
                    el = factory.element( "developer", element.getNamespace() );
                    Utils.insertAtPreferredLocation( element, el, innerCount );
                }
                updateDeveloper( value, "developer", innerCount, el );
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
        else
        {
            removeExisting( element, "developer" );
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
                                     final Collection list, final String parentTag,
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
        else
        {
            removeExisting( element, childTag );
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
                                     final Collection list, final String parentTag,
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
                                   final Collection list, final String parentTag,
                                   final String childTag )
    {
        final boolean shouldExist = ( list != null ) && ( list.size() > 0 );
        Element element = parent.getChild( parentTag, parent.getNamespace() );
        if ( element == null ) // If the list element already exists ignore it.
        {
            element = Utils.updateElement( counter, parent, parentTag, shouldExist );
        }
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
        else
        {
            removeExisting( element, childTag );
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
                                       final Collection list, final String parentTag,
                                       final String childTag )
    {
        final boolean shouldExist = ( list != null ) && ( list.size() > 0 );
        Element element = parent.getChild( parentTag, parent.getNamespace() );
        if ( element == null ) // If the list element already exists ignore it.
        {
            element = Utils.updateElement( counter, parent, parentTag, shouldExist );
        }
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
        else
        {
            removeExisting( element, childTag );
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
                                    final Collection list, final String parentTag,
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
        else
        {
            removeExisting( element, childTag );
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
                                  final Collection list, final String parentTag,
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
        else
        {
            removeExisting( element, childTag );
        }
    } // -- void iteratePlugin( Counter, Element, java.util.Collection, java.lang.String, java.lang.String )

    /**
     * Method iteratePluginExecution.
     *  @param counter
     * @param parent
     * @param list
     * @param childTag
     */
    protected void iteratePluginExecution( final IndentationCounter counter, final Element parent,
                                           final Collection list, final String childTag )
    {
        final boolean shouldExist = ( list != null ) && ( list.size() > 0 );
        final Element element = Utils.updateElement( counter, parent, "executions", shouldExist );
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
        else
        {
            removeExisting( element, childTag );
        }
    } // -- void iteratePluginExecution( Counter, Element, java.util.Collection, java.lang.String, java.lang.String )

    /**
     * Method iterateProfile.
     * @param counter
     * @param parent
     * @param list
     */
    protected void iterateProfile( final IndentationCounter counter, final Element parent, final Collection list )
    {
        final boolean shouldExist = ( list != null ) && ( list.size() > 0 );
        Element element = parent.getChild( "profiles", parent.getNamespace() );
        if ( element == null ) // If the list element already exists ignore it.
        {
            element = Utils.updateElement( counter, parent, "profiles", shouldExist );
        }
        if ( shouldExist )
        {
            final Iterator it = list.iterator();
            Iterator elIt = element.getChildren( "profile", element.getNamespace() ).iterator();
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
                    el = factory.element( "profile", element.getNamespace() );
                    Utils.insertAtPreferredLocation( element, el, innerCount );
                }
                updateProfile( value, "profile", innerCount, el );
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
        else
        {
            removeExisting( element, "profile" );
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
                                        final Collection list, final String parentTag,
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
        else
        {
            removeExisting( element, childTag );
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
                                     final Collection list, final String parentTag,
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
        else
        {
            removeExisting( element, childTag );
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
                                      final Collection list, final String parentTag,
                                      final String childTag )
    {
        // If list size > zero then there is either stuff to add or things have been removed.
        final boolean shouldExist = ( list != null ) && ( list.size() > 0 );
        Element element = parent.getChild( parentTag, parent.getNamespace() );
        if ( element == null ) // If the list element already exists ignore it.
        {
            element = Utils.updateElement( counter, parent, parentTag, shouldExist );
        }
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
        else
        {
            removeExisting( element, childTag );
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
                                    final Collection list, final String parentTag,
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
        else
        {
            removeExisting( element, childTag );
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
                                               !activation.isActiveByDefault() ? null
                                                         : String.valueOf( activation.isActiveByDefault() ),
                                               "false" );
            Utils.findAndReplaceSimpleElement( innerCount,
                                               root,
                                               "jdk", activation.getJdk(),
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
            Utils.findAndReplaceSimpleElement( innerCount, root, "missing", activationFile.getMissing(), null );
            Utils.findAndReplaceSimpleElement( innerCount, root, "exists", activationFile.getExists(), null );
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
                                               "name", activationOS.getName(),
                                               null );
            Utils.findAndReplaceSimpleElement( innerCount, root, "family", activationOS.getFamily(), null );
            Utils.findAndReplaceSimpleElement( innerCount,
                                               root,
                                               "arch", activationOS.getArch(),
                                               null );
            Utils.findAndReplaceSimpleElement( innerCount, root, "version", activationOS.getVersion(), null );
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
            Utils.findAndReplaceSimpleElement( innerCount, root, "name", activationProperty.getName(), null );
            Utils.findAndReplaceSimpleElement( innerCount, root, "value", activationProperty.getValue(), null );
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
            Utils.findAndReplaceSimpleElement( innerCount, root, "sourceDirectory", build.getSourceDirectory(), null );
            Utils.findAndReplaceSimpleElement( innerCount,
                                               root,
                                               "scriptSourceDirectory", build.getScriptSourceDirectory(),
                                               null );
            Utils.findAndReplaceSimpleElement( innerCount,
                                               root,
                                               "testSourceDirectory", build.getTestSourceDirectory(),
                                               null );
            Utils.findAndReplaceSimpleElement( innerCount, root, "outputDirectory", build.getOutputDirectory(), null );
            Utils.findAndReplaceSimpleElement( innerCount,
                                               root,
                                               "testOutputDirectory", build.getTestOutputDirectory(),
                                               null );
            iterateExtension( innerCount, root, build.getExtensions(), "extensions", "extension" );
            Utils.findAndReplaceSimpleElement( innerCount,
                                               root,
                                               "defaultGoal", build.getDefaultGoal(),
                                               null );
            iterateResource( innerCount, root, build.getResources(), "resources", "resource" );
            iterateResource( innerCount, root, build.getTestResources(), "testResources", "testResource" );
            Utils.findAndReplaceSimpleElement( innerCount,
                                               root,
                                               "directory", build.getDirectory(),
                                               null );
            Utils.findAndReplaceSimpleElement( innerCount,
                                               root,
                                               "finalName", build.getFinalName(),
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
            Utils.findAndReplaceSimpleElement( innerCount, root, "defaultGoal", buildBase.getDefaultGoal(), null );
            iterateResource( innerCount, root, buildBase.getResources(), "resources", "resource" );
            iterateResource( innerCount, root, buildBase.getTestResources(), "testResources", "testResource" );
            Utils.findAndReplaceSimpleElement( innerCount, root, "directory", buildBase.getDirectory(), null );
            Utils.findAndReplaceSimpleElement( innerCount, root, "finalName", buildBase.getFinalName(), null );
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
            Utils.findAndReplaceSimpleElement( innerCount, root, "system", ciManagement.getSystem(), null );
            Utils.findAndReplaceSimpleElement( innerCount,
                                               root,
                                               "url", ciManagement.getUrl(),
                                               null );
            iterateNotifier( innerCount, root, ciManagement.getNotifiers(), "notifiers", "notifier" );
        }
    } // -- void updateCiManagement( CiManagement, String, Counter, Element )

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
        final IndentationCounter innerCount = new IndentationCounter( counter.getDepth() + 1 );
        Utils.findAndReplaceSimpleElement( innerCount, element,
                                           "name", contributor.getName(),
                                           null );
        Utils.findAndReplaceSimpleElement( innerCount, element,
                                           "email", contributor.getEmail(),
                                           null );
        Utils.findAndReplaceSimpleElement( innerCount, element,
                                           "url", contributor.getUrl(),
                                           null );
        Utils.findAndReplaceSimpleElement( innerCount, element, "organization", contributor.getOrganization(), null );
        Utils.findAndReplaceSimpleElement( innerCount, element,
                                           "organizationUrl", contributor.getOrganizationUrl(),
                                           null );
        Utils.findAndReplaceSimpleLists( innerCount, element, contributor.getRoles(), "roles", "role" );
        Utils.findAndReplaceSimpleElement( innerCount, element, "timezone", contributor.getTimezone(), null );
        Utils.findAndReplaceProperties( innerCount, element, "properties", contributor.getProperties() );
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
        final IndentationCounter innerCount = new IndentationCounter( counter.getDepth() + 1 );
        Utils.findAndReplaceSimpleElement( innerCount, element,
                                           "groupId", dependency.getGroupId(),
                                           null );
        Utils.findAndReplaceSimpleElement( innerCount, element, "artifactId", dependency.getArtifactId(), null );
        Utils.findAndReplaceSimpleElement( innerCount, element,
                                           "version", dependency.getVersion(),
                                           null );
        Utils.findAndReplaceSimpleElement( innerCount, element,
                                           "type", dependency.getType(),
                                           "jar" );
        Utils.findAndReplaceSimpleElement( innerCount, element, "classifier", dependency.getClassifier(), null );
        Utils.findAndReplaceSimpleElement( innerCount, element,
                                           "scope", dependency.getScope(),
                                           null );
        Utils.findAndReplaceSimpleElement( innerCount, element, "systemPath", dependency.getSystemPath(), null );
        iterateExclusion( innerCount, element, dependency.getExclusions(), "exclusions", "exclusion" );
        Utils.findAndReplaceSimpleElement( innerCount, element,
                                           "optional", dependency.getOptional(),
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
                                               String.valueOf( deploymentRepository.isUniqueVersion() ),
                                               "true" );
            updateRepositoryPolicy( deploymentRepository.getReleases(), "releases", innerCount, root );
            updateRepositoryPolicy( deploymentRepository.getSnapshots(), "snapshots", innerCount, root );
            Utils.findAndReplaceSimpleElement( innerCount, root, "id", deploymentRepository.getId(), null );
            Utils.findAndReplaceSimpleElement( innerCount, root, "name", deploymentRepository.getName(), null );
            Utils.findAndReplaceSimpleElement( innerCount, root, "url", deploymentRepository.getUrl(), null );
            Utils.findAndReplaceSimpleElement( innerCount, root, "layout", deploymentRepository.getLayout(), "default" );
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
        final IndentationCounter innerCount = new IndentationCounter( counter.getDepth() + 1 );
        Utils.findAndReplaceSimpleElement( innerCount, element, "id", developer.getId(), null );
        Utils.findAndReplaceSimpleElement( innerCount, element,
                                           "name", developer.getName(),
                                           null );
        Utils.findAndReplaceSimpleElement( innerCount, element,
                                           "email", developer.getEmail(),
                                           null );
        Utils.findAndReplaceSimpleElement( innerCount, element,
                                           "url", developer.getUrl(),
                                           null );
        Utils.findAndReplaceSimpleElement( innerCount, element, "organization", developer.getOrganization(), null );
        Utils.findAndReplaceSimpleElement( innerCount, element, "organizationUrl", developer.getOrganizationUrl(), null );
        Utils.findAndReplaceSimpleLists( innerCount, element, developer.getRoles(), "roles", "role" );
        Utils.findAndReplaceSimpleElement( innerCount, element,
                                           "timezone", developer.getTimezone(),
                                           null );
        Utils.findAndReplaceProperties( innerCount, element, "properties", developer.getProperties() );
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
                                               "downloadUrl", distributionManagement.getDownloadUrl(),
                                               null );
            updateRelocation( distributionManagement.getRelocation(), "relocation", innerCount, root );
            Utils.findAndReplaceSimpleElement( innerCount, root, "status", distributionManagement.getStatus(), null );
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
        final IndentationCounter innerCount = new IndentationCounter( counter.getDepth() + 1 );
        Utils.findAndReplaceSimpleElement( innerCount, element, "artifactId", exclusion.getArtifactId(), null );
        Utils.findAndReplaceSimpleElement( innerCount, element,
                                           "groupId", exclusion.getGroupId(),
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
        final IndentationCounter innerCount = new IndentationCounter( counter.getDepth() + 1 );
        Utils.findAndReplaceSimpleElement( innerCount, element,
                                           "groupId", extension.getGroupId(),
                                           null );
        Utils.findAndReplaceSimpleElement( innerCount, element, "artifactId", extension.getArtifactId(), null );
        Utils.findAndReplaceSimpleElement( innerCount, element,
                                           "version", extension.getVersion(),
                                           null );
    } // -- void updateExtension( Extension, String, Counter, Element )

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
            Utils.findAndReplaceSimpleElement( innerCount, root, "system", issueManagement.getSystem(), null );
            Utils.findAndReplaceSimpleElement( innerCount, root, "url", issueManagement.getUrl(), null );
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
        final IndentationCounter innerCount = new IndentationCounter( counter.getDepth() + 1 );
        Utils.findAndReplaceSimpleElement( innerCount, element,
                                           "name", license.getName(),
                                           null );
        Utils.findAndReplaceSimpleElement( innerCount, element, "url", license.getUrl(), null );
        Utils.findAndReplaceSimpleElement( innerCount, element, "distribution", license.getDistribution(), null );
        Utils.findAndReplaceSimpleElement( innerCount, element,
                                           "comments", license.getComments(),
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
        final IndentationCounter innerCount = new IndentationCounter( counter.getDepth() + 1 );
        Utils.findAndReplaceSimpleElement( innerCount, element,
                                           "name", mailingList.getName(),
                                           null );
        Utils.findAndReplaceSimpleElement( innerCount, element, "subscribe", mailingList.getSubscribe(), null );
        Utils.findAndReplaceSimpleElement( innerCount, element, "unsubscribe", mailingList.getUnsubscribe(), null );
        Utils.findAndReplaceSimpleElement( innerCount, element,
                                           "post", mailingList.getPost(),
                                           null );
        Utils.findAndReplaceSimpleElement( innerCount, element,
                                           "archive", mailingList.getArchive(),
                                           null );
        Utils.findAndReplaceSimpleLists( innerCount, element, mailingList.getOtherArchives(), "otherArchives", "otherArchive" );
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
        final IndentationCounter innerCount = new IndentationCounter( counter.getDepth() + 1 );
        Utils.findAndReplaceSimpleElement( innerCount, element,
                                           "modelVersion", model.getModelVersion(),
                                           null );
        updateParent( model.getParent(), "parent", innerCount, element );
        Utils.findAndReplaceSimpleElement( innerCount, element,
                                           "groupId", model.getGroupId(),
                                           null );
        Utils.findAndReplaceSimpleElement( innerCount, element,
                                           "artifactId", model.getArtifactId(),
                                           null );
        Utils.findAndReplaceSimpleElement( innerCount, element,
                                           "version", model.getVersion(),
                                           null );
        Utils.findAndReplaceSimpleElement( innerCount, element,
                                           "packaging", model.getPackaging(),
                                           "jar" );
        Utils.findAndReplaceSimpleElement( innerCount, element, "name", model.getName(), null );
        Utils.findAndReplaceSimpleElement( innerCount, element,
                                           "description", model.getDescription(),
                                           null );
        Utils.findAndReplaceSimpleElement( innerCount, element, "url", model.getUrl(), null );
        Utils.findAndReplaceSimpleElement( innerCount, element,
                                           "inceptionYear", model.getInceptionYear(),
                                           null );
        updateOrganization( model.getOrganization(), "organization", innerCount, element );
        iterateLicense( innerCount, element, model.getLicenses(), "licenses", "license" );
        iterateDeveloper( innerCount, element, model.getDevelopers() );
        iterateContributor( innerCount, element, model.getContributors() );
        iterateMailingList( innerCount, element, model.getMailingLists(), "mailingLists", "mailingList" );
        updatePrerequisites( model.getPrerequisites(), "prerequisites", innerCount, element );
        Utils.findAndReplaceSimpleLists( innerCount, element, model.getModules(), "modules", "module" );
        updateScm( model.getScm(), "scm", innerCount, element );
        updateIssueManagement( model.getIssueManagement(), "issueManagement", innerCount, element );
        updateCiManagement( model.getCiManagement(), "ciManagement", innerCount, element );
        updateDistributionManagement( model.getDistributionManagement(), "distributionManagement", innerCount, element );
        Utils.findAndReplaceProperties( innerCount, element, "properties", model.getProperties() );
        updateDependencyManagement( model.getDependencyManagement(), "dependencyManagement", innerCount, element );
        iterateDependency( innerCount, element, model.getDependencies(), "dependencies", "dependency" );
        iterateRepository( innerCount, element, model.getRepositories(), "repositories", "repository" );
        iterateRepository( innerCount, element, model.getPluginRepositories(), "pluginRepositories", "pluginRepository" );
        updateBuild( model.getBuild(), "build", innerCount, element );
        Utils.findAndReplaceXpp3DOM( innerCount, element, "reports", (Xpp3Dom) model.getReports() );
        updateReporting( model.getReporting(), "reporting", innerCount, element );
        iterateProfile( innerCount, element, model.getProfiles() );
    } // -- void updateModel( Model, String, Counter, Element )

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
        final IndentationCounter innerCount = new IndentationCounter( counter.getDepth() + 1 );
        Utils.findAndReplaceSimpleElement( innerCount, element,
                                           "type", notifier.getType(),
                                           "mail" );
        Utils.findAndReplaceSimpleElement( innerCount, element,
                                           "sendOnError",
                                           String.valueOf( notifier.isSendOnError() ),
                                           "true" );
        Utils.findAndReplaceSimpleElement( innerCount, element, "sendOnFailure", String.valueOf( notifier.isSendOnFailure() ), "true" );
        Utils.findAndReplaceSimpleElement( innerCount, element, "sendOnSuccess", String.valueOf( notifier.isSendOnSuccess() ), "true" );
        Utils.findAndReplaceSimpleElement( innerCount, element, "sendOnWarning", String.valueOf( notifier.isSendOnWarning() ), "true" );
        Utils.findAndReplaceSimpleElement( innerCount, element,
                                           "address", notifier.getAddress(),
                                           null );
        Utils.findAndReplaceProperties( innerCount, element, "configuration", notifier.getConfiguration() );
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
                                               "name", organization.getName(),
                                               null );
            Utils.findAndReplaceSimpleElement( innerCount,
                                               root,
                                               "url", organization.getUrl(),
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
                                               "artifactId", parent.getArtifactId(),
                                               null );
            Utils.findAndReplaceSimpleElement( innerCount,
                                               root,
                                               "groupId", parent.getGroupId(),
                                               null );
            Utils.findAndReplaceSimpleElement( innerCount,
                                               root,
                                               "version", parent.getVersion(),
                                               null );
            Utils.findAndReplaceSimpleElement( innerCount, root, "relativePath", parent.getRelativePath(), "../pom.xml" );
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
        final IndentationCounter innerCount = new IndentationCounter( counter.getDepth() + 1 );
        Utils.findAndReplaceSimpleElement( innerCount, element,
                                           "groupId", plugin.getGroupId(),
                                           "org.apache.maven.plugins" );
        Utils.findAndReplaceSimpleElement( innerCount, element,
                                           "artifactId", plugin.getArtifactId(),
                                           null );
        Utils.findAndReplaceSimpleElement( innerCount, element,
                                           "version", plugin.getVersion(),
                                           null );
        Utils.findAndReplaceSimpleElement( innerCount, element,
                                           "extensions", plugin.getExtensions(),
                                           null );
        iteratePluginExecution( innerCount, element, plugin.getExecutions(), "execution" );
        iterateDependency( innerCount, element, plugin.getDependencies(), "dependencies", "dependency" );
        Utils.findAndReplaceXpp3DOM( innerCount, element, "goals", (Xpp3Dom) plugin.getGoals() );
        Utils.findAndReplaceSimpleElement( innerCount, element,
                                           "inherited", plugin.getInherited(),
                                           null );
        Utils.findAndReplaceXpp3DOM( innerCount, element, "configuration", (Xpp3Dom) plugin.getConfiguration() );
    } // -- void updatePlugin( Plugin, String, Counter, Element )

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
        final IndentationCounter innerCount = new IndentationCounter( counter.getDepth() + 1 );
        Utils.findAndReplaceSimpleElement( innerCount, element,
                                           "id", pluginExecution.getId(),
                                           "default" );
        Utils.findAndReplaceSimpleElement( innerCount, element, "phase", pluginExecution.getPhase(), null );
        Utils.findAndReplaceSimpleLists( innerCount, element, pluginExecution.getGoals(), "goals", "goal" );
        Utils.findAndReplaceSimpleElement( innerCount, element, "inherited", pluginExecution.getInherited(), null );
        Utils.findAndReplaceXpp3DOM( innerCount, element, "configuration", (Xpp3Dom) pluginExecution.getConfiguration() );
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
            Utils.findAndReplaceSimpleElement( innerCount, root, "maven", prerequisites.getMaven(), "2.0" );
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
        final IndentationCounter innerCount = new IndentationCounter( counter.getDepth() + 1 );
        Utils.findAndReplaceSimpleElement( innerCount, element,
                                           "id", profile.getId(),
                                           "default" );
        updateActivation( profile.getActivation(), "activation", innerCount, element );
        updateBuildBase( profile.getBuild(), "build", innerCount, element );
        Utils.findAndReplaceSimpleLists( innerCount, element, profile.getModules(), "modules", "module" );
        updateDistributionManagement( profile.getDistributionManagement(), "distributionManagement", innerCount,
                                      element );
        Utils.findAndReplaceProperties( innerCount, element, "properties", profile.getProperties() );
        updateDependencyManagement( profile.getDependencyManagement(), "dependencyManagement", innerCount, element );
        iterateDependency( innerCount, element, profile.getDependencies(), "dependencies", "dependency" );
        iterateRepository( innerCount, element, profile.getRepositories(), "repositories", "repository" );
        iterateRepository( innerCount, element, profile.getPluginRepositories(), "pluginRepositories", "pluginRepository" );
        Utils.findAndReplaceXpp3DOM( innerCount, element, "reports", (Xpp3Dom) profile.getReports() );
        updateReporting( profile.getReporting(), "reporting", innerCount, element );
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
            Utils.findAndReplaceSimpleElement( innerCount, root, "groupId", relocation.getGroupId(), null );
            Utils.findAndReplaceSimpleElement( innerCount, root, "artifactId", relocation.getArtifactId(), null );
            Utils.findAndReplaceSimpleElement( innerCount, root, "version", relocation.getVersion(), null );
            Utils.findAndReplaceSimpleElement( innerCount, root, "message", relocation.getMessage(), null );
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
        final IndentationCounter innerCount = new IndentationCounter( counter.getDepth() + 1 );
        Utils.findAndReplaceSimpleElement( innerCount, element, "groupId", reportPlugin.getGroupId(), "org.apache.maven.plugins" );
        Utils.findAndReplaceSimpleElement( innerCount, element, "artifactId", reportPlugin.getArtifactId(), null );
        Utils.findAndReplaceSimpleElement( innerCount, element, "version", reportPlugin.getVersion(), null );
        iterateReportSet( innerCount, element, reportPlugin.getReportSets(), "reportSets", "reportSet" );
        Utils.findAndReplaceSimpleElement( innerCount, element, "inherited", reportPlugin.getInherited(), null );
        Utils.findAndReplaceXpp3DOM( innerCount, element, "configuration", (Xpp3Dom) reportPlugin.getConfiguration() );
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
        final IndentationCounter innerCount = new IndentationCounter( counter.getDepth() + 1 );
        Utils.findAndReplaceSimpleElement( innerCount, element,
                                           "id", reportSet.getId(),
                                           "default" );
        Utils.findAndReplaceSimpleLists( innerCount, element, reportSet.getReports(), "reports", "report" );
        Utils.findAndReplaceSimpleElement( innerCount, element,
                                           "inherited", reportSet.getInherited(),
                                           null );
        Utils.findAndReplaceXpp3DOM( innerCount, element, "configuration", (Xpp3Dom) reportSet.getConfiguration() );
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
                                               "excludeDefaults", reporting.getExcludeDefaults(),
                                               null );
            Utils.findAndReplaceSimpleElement( innerCount,
                                               root,
                                               "outputDirectory", reporting.getOutputDirectory(),
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
        final IndentationCounter innerCount = new IndentationCounter( counter.getDepth() + 1 );
        updateRepositoryPolicy( repository.getReleases(), "releases", innerCount, element );
        updateRepositoryPolicy( repository.getSnapshots(), "snapshots", innerCount, element );
        Utils.findAndReplaceSimpleElement( innerCount, element,
                                           "id", repository.getId(),
                                           null );
        Utils.findAndReplaceSimpleElement( innerCount, element,
                                           "name", repository.getName(),
                                           null );
        Utils.findAndReplaceSimpleElement( innerCount, element,
                                           "url", repository.getUrl(),
                                           null );
        Utils.findAndReplaceSimpleElement( innerCount, element,
                                           "layout", repository.getLayout(),
                                           "default" );
    } // -- void updateRepository( Repository, String, Counter, Element )

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
            Utils.findAndReplaceSimpleElement( innerCount, root, "enabled", repositoryPolicy.getEnabled(), null );
            Utils.findAndReplaceSimpleElement( innerCount,
                                               root,
                                               "updatePolicy", repositoryPolicy.getUpdatePolicy(),
                                               null );
            Utils.findAndReplaceSimpleElement( innerCount,
                                               root,
                                               "checksumPolicy", repositoryPolicy.getChecksumPolicy(),
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
        final IndentationCounter innerCount = new IndentationCounter( counter.getDepth() + 1 );
        Utils.findAndReplaceSimpleElement( innerCount, element,
                                           "targetPath", resource.getTargetPath(),
                                           null );
        Utils.findAndReplaceSimpleElement( innerCount, element,
                                           "filtering", resource.getFiltering(),
                                           null );
        Utils.findAndReplaceSimpleElement( innerCount, element,
                                           "directory", resource.getDirectory(),
                                           null );
        Utils.findAndReplaceSimpleLists( innerCount, element, resource.getIncludes(), "includes", "include" );
        Utils.findAndReplaceSimpleLists( innerCount, element, resource.getExcludes(), "excludes", "exclude" );
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
                                               "connection", scm.getConnection(),
                                               null );
            Utils.findAndReplaceSimpleElement( innerCount,
                                               root,
                                               "developerConnection", scm.getDeveloperConnection(),
                                               null );
            Utils.findAndReplaceSimpleElement( innerCount, root, "tag", scm.getTag(), "HEAD" );
            Utils.findAndReplaceSimpleElement( innerCount, root, "url", scm.getUrl(), null );
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
            Utils.findAndReplaceSimpleElement( innerCount, root, "id", site.getId(), null );
            Utils.findAndReplaceSimpleElement( innerCount, root, "name", site.getName(), null );
            Utils.findAndReplaceSimpleElement( innerCount, root, "url", site.getUrl(), null );
        }
    } // -- void updateSite( Site, String, Counter, Element )

    protected void update( final Model source, final IndentationCounter indentationCounter, final Element rootElement )
    {
        updateModel( source, "project", indentationCounter, rootElement );
    }

    /**
     * Remove all existing child elements. Useful for when the Model has _removed_ elements.
     * @param element the element to search
     * @param tag the tag to search on
     */
    private void removeExisting (Element element, String tag)
    {
        if ( element != null )
        {
            logger.debug ("Removing pre-existing for " + element);
            Iterator elIt = element.getChildren( tag, element.getNamespace() ).iterator();

            while ( elIt.hasNext() )
            {
                elIt.next();
                elIt.remove();
            }
        }
    }
}
