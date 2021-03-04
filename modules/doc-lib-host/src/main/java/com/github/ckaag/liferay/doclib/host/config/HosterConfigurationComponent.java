package com.github.ckaag.liferay.doclib.host.config;

import aQute.lib.strings.Strings;
import com.liferay.document.library.kernel.model.DLFileEntry;
import com.liferay.document.library.kernel.model.DLFolder;
import com.liferay.document.library.kernel.service.DLFileEntryLocalService;
import com.liferay.document.library.kernel.service.DLFileEntryService;
import com.liferay.document.library.kernel.service.DLFolderLocalService;
import com.liferay.portal.configuration.metatype.bnd.util.ConfigurableUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import org.osgi.service.component.annotations.*;

import java.util.Map;

@Component(configurationPid = {
                HosterConfigurationComponent.CONFIG_ID}, configurationPolicy = ConfigurationPolicy.OPTIONAL, immediate = true, service = HostDirectoriesSource.class)
public class HosterConfigurationComponent implements HostDirectoriesSource {
    public static final String CONFIG_ID = "com.github.ckaag.liferay.doclib.host.config.HosterConfiguration";
    private static final Log logger = LogFactoryUtil.getLog(HosterConfigurationComponent.class);

    @Reference
    private DLFileEntryLocalService dlFileEntryLocalService;

    @Reference
    private DLFolderLocalService dlFolderLocalService;

    @Reference
    private DLFileEntryService dlFileEntryService;

    private volatile HosterConfiguration _configuration;

    @Activate
    @Modified
    protected void activate(Map<String, Object> properties) {
        _configuration = ConfigurableUtil.createConfigurable(
                        HosterConfiguration.class, properties);
        logger.info("Serving following folderIds: " + Strings.join(",",_configuration.hostedFolderIds()));
    }


    private Long getDLFileEntryId(String folderName, String fileName) {
        HosterConfiguration conf = this._configuration;
        String[] folders = conf.hostedFolderIds();
        for (String folderIdStr : folders) {
            long folderId = Long.parseLong(folderIdStr);
            DLFolder folder = dlFolderLocalService.fetchDLFolder(folderId);
            if (folder != null && matchesName(folder, folderName)) {
                DLFileEntry file = dlFileEntryLocalService.fetchFileEntry(folder.getGroupId(), folder.getFolderId(), unescapeFileTitle(fileName));
                if (file != null) {
                    return file.getFileEntryId();
                } else {
                    return null;
                }
            }
        }
        return null;
    }

    private String unescapeFileTitle(String fileName) {
        return fileName;
    }

    private boolean matchesName(DLFolder folder, String folderName) {
        return folder.getName().equalsIgnoreCase(folderName);
    }

    @Override
    public DLFileEntry getFileOrNull(long companyId, String folderName, String fileName) throws PortalException {
        Long id = getDLFileEntryId(folderName, fileName);
        return id != null ? this.dlFileEntryService.getFileEntry(id) : null;
    }
}
