package fr.adrienbrault.idea.symfony2plugin.profiler.utils;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.intellij.lang.html.HTMLLanguage;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.impl.source.html.HtmlFileImpl;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlTag;
import com.intellij.psi.xml.XmlTagValue;
import fr.adrienbrault.idea.symfony2plugin.profiler.collector.HttpDefaultDataCollector;
import fr.adrienbrault.idea.symfony2plugin.profiler.dict.HttpProfilerRequest;
import fr.adrienbrault.idea.symfony2plugin.profiler.dict.ProfilerRequestInterface;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ProfilerUtil {

    /**
     * Cache for url content
     */
    private static Cache<String, String> REQUEST_CACHE = CacheBuilder.newBuilder()
        .maximumSize(50)
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .build();

    private static Cache<String, ProfilerRequestInterface> PROFILER_REQUEST_CACHE = CacheBuilder.newBuilder()
        .maximumSize(15)
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .build();

    /**
     * Extract "table.search-results tbody tr td"
     * We dont have complete xpath with html support inside so reuse internal html parser
     */
    @NotNull
    public static Collection<ProfilerRequestInterface> createRequestsFromIndexHtml(@NotNull Project project, @NotNull String html, @NotNull String baseUrl) {
        HtmlFileImpl htmlFile = (HtmlFileImpl) PsiFileFactory.getInstance(project).createFileFromText(HTMLLanguage.INSTANCE, html);

        final XmlTag[] result = new XmlTag[1];

        // find table
        PsiTreeUtil.processElements(htmlFile, psiElement -> {
            if(psiElement instanceof XmlTag && "table".equals(((XmlTag) psiElement).getName()) && "search-results".equals(((XmlTag) psiElement).getAttributeValue("id"))) {
                result[0] = (XmlTag) psiElement;
                return false;
            }
            return true;
        });

        if(result[0] == null) {
            return Collections.emptyList();
        }

        // find table to be our keys for Map
        XmlTag thead = result[0].findFirstSubTag("thead");
        if(thead == null) {
            return Collections.emptyList();
        }

        XmlTag tr1 = thead.findFirstSubTag("tr");
        if(tr1 == null) {
            return Collections.emptyList();
        }

        List<String> header = new ArrayList<>();
        for (XmlTag th : tr1.findSubTags("th")) {
            header.add(StringUtils.trim(stripHtmlTags(th.getValue().getText())).toLowerCase());
        }

        // we need at least this fields
        if(!new HashSet<>(header).containsAll(Arrays.asList("token", "url"))) {
            return Collections.emptyList();
        }

        XmlTag tbody = result[0].findFirstSubTag("tbody");
        if(tbody == null) {
            return Collections.emptyList();
        }

        List<ProfilerRequestInterface> requests = new ArrayList<>();
        for (XmlTag tr : tbody.findSubTags("tr")) {

            // secure limit
            if(requests.size() >= 10) {
                break;
            }

            // "td" elements dont match header "th"
            XmlTag[] findSubTags = tr.findSubTags("td");
            if(findSubTags.length < header.size()) {
                continue;
            }

            // build row map with header keys
            Map<String, Pair<XmlTag, String>> row = new HashMap<>();
            for (int i = 0; i < findSubTags.length; i++) {
                row.put(header.get(i), Pair.create(
                    findSubTags[i],
                    StringUtils.trim(stripHtmlTags(findSubTags[i].getText().replaceAll("\\n", " "))).replace("\\s+", " ")
                ));
            }

            // extract token link to be our linked profiler url
            String profilerUrl = null;
            XmlTag tokenLink = row.get("token").getFirst().findFirstSubTag("a");
            if(tokenLink != null) {
                String href = tokenLink.getAttributeValue("href");
                if(StringUtils.isNotBlank(href)) {
                    profilerUrl = getProfilerAbsoluteUrl(baseUrl, href);
                }
            }

            // extract status code
            int statusCode = 0;
            if(row.containsKey("status")) {
                try {
                    statusCode = Integer.parseInt(row.get("status").getSecond());
                } catch (NumberFormatException ignored) {
                }
            }

            requests.add(new HttpProfilerRequest(
                statusCode,
                row.get("token").getSecond(),
                profilerUrl,
                row.containsKey("method") ? row.get("method").getSecond() : "n/a",
                row.get("url").getSecond()
            ));
        }

        return requests;
    }

    @NotNull
    private static String getProfilerAbsoluteUrl(@NotNull String baseUrl, @NotNull String href) {
        return StringUtils.stripEnd(baseUrl, "/") + href.substring(href.indexOf("/_profiler/"));
    }

    @NotNull
    public static Collection<ProfilerRequestInterface> collectHttpDataForRequest(@NotNull Project project, @NotNull Collection<ProfilerRequestInterface> requests) {
        Collection<Callable<ProfilerRequestInterface>> callable = requests.stream().map(
            request -> new MyProfilerRequestDecoratedCollectorCallable(project, request)).collect(Collectors.toCollection(ArrayList::new)
        );

        return getProfilerRequestCollectorDecorated(callable, 10);
    }

    /**
     * "_controller" and "_route"
     * "/_profiler/242e61?panel=request"
     *
     * <tr>
     *  <th>_route</th>
     *  <td>foo_route</td>
     * </tr>
     */
    @NotNull
    public static Map<String, String> getRequestAttributes(@NotNull Project project, @NotNull String html) {
        HtmlFileImpl htmlFile = (HtmlFileImpl) PsiFileFactory.getInstance(project).createFileFromText(HTMLLanguage.INSTANCE, html);

        String[] keys = new String[] {"_controller", "_route"};

        Map<String, String> map = new HashMap<>();
        PsiTreeUtil.processElements(htmlFile, psiElement -> {
            if(!(psiElement instanceof XmlTag) || !"th".equals(((XmlTag) psiElement).getName())) {
                return true;
            }

            XmlTagValue keyTag = ((XmlTag) psiElement).getValue();
            String key = StringUtils.trim(keyTag.getText());
            if(!ArrayUtils.contains(keys, key)) {
                return true;
            }

            XmlTag tdTag = PsiTreeUtil.getNextSiblingOfType(psiElement, XmlTag.class);
            if(tdTag == null || !"td".equals(tdTag.getName())) {
                return true;
            }

            XmlTagValue valueTag = tdTag.getValue();
            String value = valueTag.getText();
            if(StringUtils.isBlank(value)) {
                return true;
            }

            // Symfony 3.2 profiler debug? strip html
            map.put(key, stripHtmlTags(value));

            // exit if all item found
            return map.size() != keys.length;
        });

        return map;
    }

    /**
     * ["foo/foo.html.twig": 1]
     *
     * <tr>
     *  <td>@Twig/Exception/traces_text.html.twig</td>
     *  <td class="font-normal">1</td>
     * </tr>
     */
    public static Map<String, Integer> getRenderedElementTwigTemplates(@NotNull Project project, @NotNull String html) {
        HtmlFileImpl htmlFile = (HtmlFileImpl) PsiFileFactory.getInstance(project).createFileFromText(HTMLLanguage.INSTANCE, html);

        final XmlTag[] xmlTag = new XmlTag[1];
        PsiTreeUtil.processElements(htmlFile, psiElement -> {
            if(!(psiElement instanceof XmlTag) || !"h2".equals(((XmlTag) psiElement).getName())) {
                return true;
            }

            XmlTagValue keyTag = ((XmlTag) psiElement).getValue();
            String contents = StringUtils.trim(keyTag.getText());
            if(!"Rendered Templates".equalsIgnoreCase(contents)) {
                return true;
            }

            xmlTag[0] = (XmlTag) psiElement;

            return true;
        });

        if(xmlTag[0] == null) {
            return Collections.emptyMap();
        }

        XmlTag tableTag = PsiTreeUtil.getNextSiblingOfType(xmlTag[0], XmlTag.class);
        if(tableTag == null || !"table".equals(tableTag.getName())) {
            return Collections.emptyMap();
        }

        XmlTag tbody = tableTag.findFirstSubTag("tbody");
        if(tbody == null) {
            return Collections.emptyMap();
        }

        Map<String, Integer> templates = new HashMap<>();

        for (XmlTag tag : PsiTreeUtil.getChildrenOfTypeAsList(tbody, XmlTag.class)) {
            if(!"tr".equals(tag.getName())) {
                continue;
            }

            XmlTag[] tds = tag.findSubTags("td");
            if(tds.length < 2) {
                continue;
            }

            String template = stripHtmlTags(StringUtils.trim(tds[0].getValue().getText()));
            if(StringUtils.isBlank(template)) {
                continue;
            }

            int count;
            try {
                count = Integer.parseInt(stripHtmlTags(StringUtils.trim(tds[1].getValue().getText())));
            } catch (NumberFormatException e) {
                count = 0;
            }

            templates.put(template, count);
        }

        return templates;
    }

    @NotNull
    private static String stripHtmlTags(@NotNull String text)
    {
        return text.replaceAll("<[^>]*>", "");
    }

    @Nullable
    public static String getProfilerUrlContent(@NotNull String url) {
        URLConnection conn;
        try {
            conn = new URL(url).openConnection();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        try {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        } catch (IOException e) {
            return null;
        }
    }

    private static class MyProfilerRequestDecoratedCollectorCallable implements Callable<ProfilerRequestInterface> {
        @NotNull
        private final Project project;

        @NotNull
        private final ProfilerRequestInterface request;

        @NotNull
        private final String profilerUrl;

        MyProfilerRequestDecoratedCollectorCallable(@NotNull Project project, @NotNull ProfilerRequestInterface request) {
            this.project = project;
            this.request = request;
            this.profilerUrl = request.getProfilerUrl();
        }

        @Override
        public ProfilerRequestInterface call() throws Exception {
            ProfilerRequestInterface requestCache = PROFILER_REQUEST_CACHE.getIfPresent(profilerUrl);
            if(requestCache != null) {
                return requestCache;
            }

            ProfilerRequestInterface httpProfilerRequest = new HttpProfilerRequest(
                request,
                new HttpDefaultDataCollector(getRequestAttributes())
            );

            PROFILER_REQUEST_CACHE.put(profilerUrl, httpProfilerRequest);

            return httpProfilerRequest;
        }

        @NotNull
        private Map<String, String> getRequestAttributes() {
            Map<String, String> requestAttributes = new HashMap<>();

            String requestContent = getUrlContent(profilerUrl + "?panel=request");
            String twigContent = getUrlContent(profilerUrl + "?panel=twig");

            if(requestContent != null) {
                ApplicationManager.getApplication().runReadAction(() ->
                    requestAttributes.putAll(ProfilerUtil.getRequestAttributes(project, requestContent))
                );
            }

            if(twigContent != null) {
                ApplicationManager.getApplication().runReadAction(() -> {
                    Map<String, Integer> templates = getRenderedElementTwigTemplates(project, twigContent);
                    if(templates.size() > 0) {
                        requestAttributes.put("_template", templates.keySet().iterator().next());
                    }
                });
            }

            return requestAttributes;
        }

        private String getUrlContent(@NotNull String url) {
            String contents = REQUEST_CACHE.getIfPresent(url);

            if(contents == null) {
                contents = ProfilerUtil.getProfilerUrlContent(url);
                REQUEST_CACHE.put(url, contents);
            }

            return contents;
        }
    }

    /**
     * Decorated request model with loaded collector data
     * loads data on multiple thread to be as fast as possible
     */
    @NotNull
    public static List<ProfilerRequestInterface> getProfilerRequestCollectorDecorated(@NotNull Collection<Callable<ProfilerRequestInterface>> callable, int threads) {
        ExecutorService executor = Executors.newFixedThreadPool(threads);

        List<Future<ProfilerRequestInterface>> futures;
        try {
            futures = executor.invokeAll(callable);
        } catch (InterruptedException e) {
            return Collections.emptyList();
        }

        List<ProfilerRequestInterface> requests = new ArrayList<>();
        for (Future<ProfilerRequestInterface> future : futures) {
            try {
                requests.add(future.get());
            } catch (ExecutionException | InterruptedException ignored) {
            }
        }

        executor.shutdown();

        return requests;
    }

    /**
     * Try to find a base url profiler relative url:
     *  "/foobar" =>  "http://127.0.0.1:8000/foobar"
     *
     * In local csv context we dont know path info. There is a way
     * to try extract it from serialized string but overhead
     *
     * Think of:
     * http://127.0.0.1/
     * http://127.0.0.1:8000/
     * https://127.0.0.1:8000/
     * https://127.0.0.1:8000/app_dev.php
     */
    @Nullable
    public static String getBaseProfilerUrlFromRequest(@NotNull ProfilerRequestInterface request) {
        URL url = null;
        try {
            url = new URL(request.getProfilerUrl());
        } catch (MalformedURLException e) {
            try {
                url = new URL(request.getUrl());
            } catch (MalformedURLException ignored) {
            }
        }

        if (url == null) {
            return null;
        }

        String portValue = "";
        int port = url.getPort();
        if(port != -1 && port != 80) {
            portValue = ":" + port;
        }

        String pathSuffix = "";
        String urlPath = url.getPath();
        Matcher matcher = Pattern.compile(".*(/app_[\\w]{2,6}.php)/").matcher(urlPath);
        if(matcher.find()){
            pathSuffix = StringUtils.stripEnd(urlPath.substring(0, matcher.end()), "/");
        }

        return url.getProtocol() + "://" + url.getHost() + portValue + pathSuffix;
    }

    @Nullable
    public static String formatProfilerRow(@NotNull ProfilerRequestInterface profilerRequest) {
        int statusCode = profilerRequest.getStatusCode();

        String path = profilerRequest.getUrl();

        try {
            URL url = new URL(profilerRequest.getUrl());
            path = url.getPath();

            Matcher matcher = Pattern.compile(".*(/app_[\\w]{2,6}.php)/").matcher(path);
            if(matcher.find()){
                path = "/" + path.substring(matcher.end());
            }

        } catch (MalformedURLException ignored) {
        }

        return String.format("(%s) %s", statusCode == 0 ? "n/a" : statusCode, StringUtils.abbreviate(path, 35));
    }

    @Nullable
    public static String getContentForFile(@NotNull File file) {
        boolean isGzipFile;

        try {
            byte[] buffer = new byte[3];

            InputStream is = new FileInputStream(file);
            int __ = is.read(buffer);

            // check gzip header
            isGzipFile = buffer[0] == 31
                && buffer[1] == -117
                && buffer[2] == 8;

            is.close();
        } catch (IOException e) {
            return null;
        }

        if (isGzipFile) {
            return getProfilerContentGzdecode(file);
        }

        return getContentForRaw(file);
    }

    @Nullable
    private static String getContentForRaw(@NotNull File file) {
        StringBuilder content = new StringBuilder();

        try {
            BufferedReader in = new BufferedReader(new FileReader(file));
            String str;
            while ((str = in.readLine()) != null) {
                content.append(str);
            }
            in.close();
        } catch (IOException ignored) {
            return null;
        }

        return content.toString();
    }

    @Nullable
    private static String getProfilerContentGzdecode(File file) {
        try {
            GZIPInputStream gis;
            try (InputStream in = new FileInputStream(file.getPath())) {
                gis = new GZIPInputStream(new ByteArrayInputStream(in.readAllBytes()));
            }

            BufferedReader bf = new BufferedReader(new InputStreamReader(gis));
            StringBuilder outStr = new StringBuilder();

            String line;
            while ((line = bf.readLine()) != null) {
                outStr.append(line);
            }

            return outStr.toString();
        } catch (IOException ignored) {
        }

        return null;
    }
}

