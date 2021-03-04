package com.github.ckaag.liferay.doclib.host.config;

import aQute.bnd.annotation.metatype.Meta;

@Meta.OCD(id = HosterConfigurationComponent.CONFIG_ID, localization = "content/Language", name = "host-config-name")
public interface HosterConfiguration {

    @Meta.AD(required = false, name = "host-folder-ids", deflt = "-1")
    String[] hostedFolderIds();
}
