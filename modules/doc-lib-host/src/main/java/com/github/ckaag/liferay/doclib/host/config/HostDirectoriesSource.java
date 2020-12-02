package com.github.ckaag.liferay.doclib.host.config;

import com.liferay.document.library.kernel.model.DLFileEntry;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.theme.ThemeDisplay;

public interface HostDirectoriesSource {
    DLFileEntry getFileOrNull(long companyId, String folderName, String fileName) throws PortalException;
}
