package com.github.ckaag.liferay.doclib.host.config;

import com.liferay.document.library.kernel.model.DLFileEntry;
import com.liferay.document.library.kernel.model.DLFolder;
import com.liferay.document.library.kernel.service.DLFileEntryLocalService;
import com.liferay.document.library.kernel.service.DLFileEntryService;
import com.liferay.document.library.kernel.service.DLFolderLocalService;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Company;
import com.liferay.portal.kernel.module.configuration.ConfigurationProvider;
import com.liferay.portal.kernel.service.CompanyLocalService;
import org.osgi.service.component.annotations.*;

import java.util.HashMap;
import java.util.Map;

@Component(configurationPid = {
        "de.abiegel.configuration.osgi.company.CompanyConfiguredComponentConfig"}, configurationPolicy = ConfigurationPolicy.OPTIONAL, property = {
        "osgi.command.scope=foo", "osgi.command.function=foo"}, immediate = true, service = HostDirectoriesSource.class)
public class HosterConfigurationComponent implements HostDirectoriesSource {
    public static final String CONFIG_ID = "com.github.ckaag.liferay.doclib.host.config.HosterConfiguration";
    private static final Log logger = LogFactoryUtil.getLog(HosterConfigurationComponent.class);

    @Reference
    private ConfigurationProvider configurationProvider;

    @Reference
    private CompanyLocalService companyLocalService;

    @Reference
    private DLFileEntryLocalService dlFileEntryLocalService;

    @Reference
    private DLFolderLocalService dlFolderLocalService;

    @Reference
    private DLFileEntryService dlFileEntryService;

    private final Map<Long, HosterConfiguration> configurations = new HashMap<>();

    @Activate
    @Modified
    protected void readConfig() {
        configurations.clear();
        for (Company company : companyLocalService.getCompanies()) {
            try {
                this.configurations.put(
                        company.getCompanyId(),
                        configurationProvider
                                .getCompanyConfiguration(
                                        HosterConfiguration.class,
                                        company.getCompanyId()
                                )
                );
            } catch (Exception e) {
                logger.error("Cannot Acces config for company " + (company != null ? company.getVirtualHostname() : "null"), e);
            }
        }
    }


    private Long getDLFileEntryId(long companyId, String folderName, String fileName) {
        HosterConfiguration conf = this.configurations.get(companyId);
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
        Long id = getDLFileEntryId(companyId, folderName, fileName);
        return id != null ? this.dlFileEntryService.getFileEntry(id) : null;
    }
}
