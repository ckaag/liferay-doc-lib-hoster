package com.github.ckaag.liferay.doclib.host.config;

import java.util.List;
import java.util.Map;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;

import com.liferay.document.library.kernel.model.DLFileEntry;
import com.liferay.document.library.kernel.model.DLFolder;
import com.liferay.document.library.kernel.service.DLFileEntryLocalService;
import com.liferay.document.library.kernel.service.DLFileEntryService;
import com.liferay.document.library.kernel.service.DLFolderLocalService;
import com.liferay.portal.configuration.metatype.bnd.util.ConfigurableUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;

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
        logger.info("Serving following folderIds: " + String.join(", ", _configuration.hostedFolderIds()));
    }

    private Long getDLFileEntryId(DLFolder folder, String fileName, List<String> subfolders) {
        if (subfolders == null || subfolders.isEmpty()) {
            DLFileEntry file = dlFileEntryLocalService.fetchFileEntry(folder.getGroupId(), folder.getFolderId(), unescapeFileTitle(fileName));
            if (file != null) {
                return file.getFileEntryId();
            } else {
                return null;
            }
        } else {
            DLFolder subFolder = dlFolderLocalService.fetchFolder(folder.getGroupId(), folder.getFolderId(), subfolders.get(0));
            return getDLFileEntryId(subFolder, fileName, subfolders.subList(1, subfolders.size()));
        }
    }

    private String unescapeFileTitle(String fileName) {
        return fileName;
    }

    private boolean matchesName(DLFolder folder, String folderName) {
        return folder.getName().equalsIgnoreCase(folderName);
    }

    @Override
    public DLFileEntry getFileOrNull(long companyId, String folderName, String fileName, List<String> subfolder) throws PortalException {
        DLFolder folder = getBaseFolderId(companyId, folderName);
        Long id = folder != null ? getDLFileEntryId(folder, fileName, subfolder) : null;
        return id != null ? this.dlFileEntryService.getFileEntry(id) : null;
    }

    private DLFolder getBaseFolderId(long companyId, String folderName) {
        HosterConfiguration conf = this._configuration;
        String[] folders = conf.hostedFolderIds();
        for (String folderIdStr : folders) {
            long folderId = Long.parseLong(folderIdStr);
            DLFolder folder = dlFolderLocalService.fetchDLFolder(folderId);
            if (folder != null && matchesName(folder, folderName) && folder.getCompanyId() == companyId) {
                return folder;
            }
        }
        return null;
    }
}
