/*
 * Copyright 2007-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.ymate.platform.webmvc.impl;

import net.ymate.platform.core.YMP;
import net.ymate.platform.core.lang.BlurObject;
import net.ymate.platform.core.util.ClassUtils;
import net.ymate.platform.core.util.RuntimeUtils;
import net.ymate.platform.webmvc.*;
import net.ymate.platform.webmvc.base.Type;
import org.apache.commons.fileupload.ProgressListener;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 默认WebMVC模块配置接口实现
 *
 * @author 刘镇 (suninformation@163.com) on 15/5/28 下午1:35
 * @version 1.0
 */
public class DefaultModuleCfg implements IWebMvcModuleCfg {

    private static final String __IGNORE = "^.+\\.(jsp|jspx|png|gif|jpg|jpeg|js|css|swf|ico|htm|html|eot|woff|woff2|ttf|svg)$";

    private IRequestProcessor __requestProcessor;

    private IWebErrorProcessor __errorProcessor;

    private IWebCacheProcessor __cacheProcessor;

    private String __charsetEncoding;

    private String __requestIgnoreRegex;

    private String __requestMethodParam;

    private String __requestPrefix;

    private boolean __parameterEscapeMode;

    private String __baseViewPath;

    private String __abstractBaseViewPath;

    private String __cookiePrefix;

    private String __cookieDomain;

    private String __cookiePath;

    private String __cookieAuthKey;

    private String __uploadTempDir;

    private int __uploadFileSizeMax;

    private int __uploadTotalSizeMax;

    private int __uploadSizeThreshold;

    private ProgressListener __uploadFileListener;

    private boolean __conventionMode;

    private boolean __conventionUrlrewriteMode;

    private boolean __conventionInterceptorMode;

    private Set<String> __conventionViewAllowPaths;

    private Set<String> __conventionViewNotAllowPaths;

    public DefaultModuleCfg(YMP owner) throws Exception {
        Map<String, String> _moduleCfgs = owner.getConfig().getModuleConfigs(IWebMvc.MODULE_NAME);
        //
        String _reqProcessorClass = StringUtils.defaultIfBlank(_moduleCfgs.get("request_processor_class"), "default");
        Class<? extends IRequestProcessor> _requestProcessorClass = Type.REQUEST_PROCESSORS.get(_reqProcessorClass);
        if (_requestProcessorClass == null && StringUtils.isNotBlank(_reqProcessorClass)) {
            __requestProcessor = ClassUtils.impl(_reqProcessorClass, IRequestProcessor.class, this.getClass());
        } else if (_requestProcessorClass != null) {
            __requestProcessor = _requestProcessorClass.newInstance();
        }
        if (__requestProcessor == null) {
            __requestProcessor = new DefaultRequestProcessor();
        }
        //
        String _errorProcessorClass = _moduleCfgs.get("error_processor_class");
        if (StringUtils.isNotBlank(_errorProcessorClass)) {
            __errorProcessor = ClassUtils.impl(_errorProcessorClass, IWebErrorProcessor.class, this.getClass());
        }
        //
        String _cacheProcessorClass = _moduleCfgs.get("cache_processor_class");
        if (StringUtils.isNotBlank(_errorProcessorClass)) {
            __cacheProcessor = ClassUtils.impl(_cacheProcessorClass, IWebCacheProcessor.class, this.getClass());
        }
        //
        __charsetEncoding = StringUtils.defaultIfBlank(_moduleCfgs.get("default_charset_encoding"), "UTF-8");
        __requestIgnoreRegex = StringUtils.defaultIfBlank(_moduleCfgs.get("request_ignore_regex"), __IGNORE);
        __requestMethodParam = StringUtils.defaultIfBlank(_moduleCfgs.get("request_method_param"), "_method");
        __requestPrefix = StringUtils.trimToEmpty(_moduleCfgs.get("request_prefix"));
        //
        __parameterEscapeMode = BlurObject.bind(_moduleCfgs.get("parameter_escape_mode")).toBooleanValue();
        //
        __baseViewPath = RuntimeUtils.replaceEnvVariable(StringUtils.defaultIfBlank(_moduleCfgs.get("base_view_path"), "/WEB-INF/templates/"));
        __abstractBaseViewPath = __baseViewPath;
        if (__abstractBaseViewPath.startsWith("/WEB-INF")) {
            __abstractBaseViewPath = new File(RuntimeUtils.getRootPath(false), __abstractBaseViewPath).getPath();
        }
        //
        __cookiePrefix = StringUtils.trimToEmpty(_moduleCfgs.get("cookie_prefix"));
        __cookieDomain = StringUtils.trimToEmpty(_moduleCfgs.get("cookie_domain"));
        __cookiePath = StringUtils.defaultIfBlank(_moduleCfgs.get("cookie_path"), "/");
        __cookieAuthKey = StringUtils.trimToEmpty(_moduleCfgs.get("cookie_auth_key"));
        //
        __uploadTempDir = RuntimeUtils.replaceEnvVariable(StringUtils.trimToEmpty(_moduleCfgs.get("upload_temp_dir")));
        __uploadFileSizeMax = Integer.parseInt(StringUtils.defaultIfBlank(_moduleCfgs.get("upload_file_size_max"), "10485760"));
        __uploadTotalSizeMax = Integer.parseInt(StringUtils.defaultIfBlank(_moduleCfgs.get("upload_total_size_max"), "10485760"));
        __uploadSizeThreshold = Integer.parseInt(StringUtils.defaultIfBlank(_moduleCfgs.get("upload_size_threshold"), "10240"));
        __uploadFileListener = ClassUtils.impl(_moduleCfgs.get("upload_file_listener_class"), ProgressListener.class, this.getClass());
        //
        __conventionMode = BlurObject.bind(_moduleCfgs.get("convention_mode")).toBooleanValue();
        __conventionUrlrewriteMode = BlurObject.bind(_moduleCfgs.get("convention_urlrewrite_mode")).toBooleanValue();
        __conventionInterceptorMode = BlurObject.bind(_moduleCfgs.get("convention_interceptor_mode")).toBooleanValue();
        //
        __conventionViewAllowPaths = new HashSet<String>();
        __conventionViewNotAllowPaths = new HashSet<String>();
        //
        String[] _cViewPaths = StringUtils.split(StringUtils.defaultIfBlank(_moduleCfgs.get("convention_view_paths"), ""), "|");
        if (_cViewPaths != null) {
            for (String _cvPath : _cViewPaths) {
                _cvPath = StringUtils.trimToNull(_cvPath);
                if (_cvPath != null) {
                    boolean _flag = true;
                    if (_cvPath.length() > 1) {
                        char _c = _cvPath.charAt(_cvPath.length() - 1);
                        if (_c == '+') {
                            _cvPath = StringUtils.substring(_cvPath, 0, _cvPath.length() - 1);
                        } else if (_c == '-') {
                            _cvPath = StringUtils.substring(_cvPath, 0, _cvPath.length() - 1);
                            _flag = false;
                        }
                    }
                    if (_cvPath.charAt(0) != '/') {
                        _cvPath = "/" + _cvPath;
                    }
                    if (_flag) {
                        __conventionViewAllowPaths.add(_cvPath);
                    } else {
                        __conventionViewNotAllowPaths.add(_cvPath);
                    }
                }
            }
        }
    }

    public IRequestProcessor getRequestProcessor() {
        return __requestProcessor;
    }

    public IWebErrorProcessor getErrorProcessor() {
        return __errorProcessor;
    }

    public IWebCacheProcessor getCacheProcessor() {
        return __cacheProcessor;
    }

    public String getDefaultCharsetEncoding() {
        return __charsetEncoding;
    }

    public String getRequestIgnoreRegex() {
        return __requestIgnoreRegex;
    }

    public String getRequestMethodParam() {
        return __requestMethodParam;
    }

    public String getRequestPrefix() {
        return __requestPrefix;
    }

    public String getBaseViewPath() {
        return __baseViewPath;
    }

    public String getAbstractBaseViewPath() {
        return __abstractBaseViewPath;
    }

    public String getCookiePrefix() {
        return __cookiePrefix;
    }

    public String getCookieDomain() {
        return __cookieDomain;
    }

    public String getCookiePath() {
        return __cookiePath;
    }

    public String getCookieAuthKey() {
        return __cookieAuthKey;
    }

    public String getUploadTempDir() {
        return __uploadTempDir;
    }

    public int getUploadFileSizeMax() {
        return __uploadFileSizeMax;
    }

    public int getUploadTotalSizeMax() {
        return __uploadTotalSizeMax;
    }

    public int getUploadSizeThreshold() {
        return __uploadSizeThreshold;
    }

    public ProgressListener getUploadFileListener() {
        return __uploadFileListener;
    }

    public boolean isConventionMode() {
        return __conventionMode;
    }

    public boolean isConventionUrlrewriteMode() {
        return __conventionUrlrewriteMode;
    }

    public boolean isConventionInterceptorMode() {
        return __conventionInterceptorMode;
    }

    public Set<String> getConventionViewAllowPaths() {
        return Collections.unmodifiableSet(__conventionViewAllowPaths);
    }

    public Set<String> getConventionViewNotAllowPaths() {
        return Collections.unmodifiableSet(__conventionViewNotAllowPaths);
    }

    public boolean isParameterEscapeMode() {
        return __parameterEscapeMode;
    }
}
