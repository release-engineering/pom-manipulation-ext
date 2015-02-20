/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/

package org.commonjava.maven.ext.manip.util;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.TreeMap;

import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Custom limited map implementation that handles the following format:
 * <p>
 *     String(groupId) : Map<br/>
 *     where Map contains String(artifactId):String(value)
 * </p>
 * artifactId may be a wildcard (*) or an explicit value.
 */
public class WildcardMap
{
    private static final String WILDCARD = "*";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * This map represents:<br/>
     * groupId : map where map is artifactId : value
     * <br/>
     * artifactId may be a wildcard '*'.
     */
    private final TreeMap<String, LinkedHashMap<String,String>> map = new TreeMap<String, LinkedHashMap<String, String>>();

    /**
     * Returns <tt>true</tt> if this map contains a mapping for the specified
     * key.
     *
     */
    public boolean containsKey(ProjectRef key)
    {
        String groupId = key.getGroupId();
        String artifactId = key.getArtifactId();
        boolean result;

        LinkedHashMap vMap = map.get(groupId);

        if ( vMap == null || vMap.size() == 0)
        {
            result = false;
        }
        else
        {
            if ( vMap.get(WILDCARD) != null)
            {
                result = true;
            }
            else
            {
                result = vMap.containsKey(artifactId);
            }
        }
        return result;
    }


    /**
     * Associates the specified value with the specified key in this map.
     */
    public void put(ProjectRef key, String value)
    {
        String groupId = key.getGroupId();
        String artifactId = key.getArtifactId();

        LinkedHashMap vMap = map.get(groupId);
        if ( vMap == null)
        {
            vMap = new LinkedHashMap();
        }
        boolean wildcard = false;

        if ( WILDCARD.equals(artifactId))
        {
            // Erase any previous mappings.
            if ( vMap.size() > 0)
            {
                logger.warn ("Emptying map with keys " + vMap.keySet() + " as replacing with wildcard mapping " + key);
            }
            vMap.clear();
        }
        else
        {
            Iterator i = vMap.keySet().iterator();
            while (i.hasNext())
            {
                if (i.next().equals(WILDCARD))
                {
                    wildcard = true;
                }
            }
        }
        if ( wildcard )
        {
            logger.warn ("Unable to add " + key + " with value " + value +
                    " as wildcard mapping for " + groupId + " already exists.");
        }
        else
        {
            logger.debug ("Entering artifact of " + artifactId + " and value " + value);
            vMap.put(artifactId, value);

            map.put(groupId, vMap);
        }
    }


    /**
     * Returns the value to which the specified key is mapped,
     * or {@code null} if this map contains no mapping for the key.
     * <p/>
     * Takes groupId:artifactId key which is split to index purely
     * by groupId.
     */
    public String get(ProjectRef key)
    {
        String groupId = key.getGroupId();
        String artifactId = key.getArtifactId();
        String result = null;

        LinkedHashMap<String, String> value = map.get(groupId);
        if (value != null)
        {
            logger.debug("Retrieved value map of " + value);
            if ( value.get(WILDCARD) != null)
            {
                result = value.get(WILDCARD);
            }
            else
            {
                result = value.get(artifactId);
            }
        }
        logger.debug("Returning result of " + result);

        return result;
    }

    @Override
    public String toString()
    {
        return "WildcardMap{" +
                "map=" + map +
                '}';
    }
}
