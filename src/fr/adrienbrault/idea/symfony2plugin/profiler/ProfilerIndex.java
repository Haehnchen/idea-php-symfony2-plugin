package fr.adrienbrault.idea.symfony2plugin.profiler;

import com.intellij.openapi.vfs.VfsUtil;
import fr.adrienbrault.idea.symfony2plugin.profiler.dict.ProfilerRequest;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Vector;

public class ProfilerIndex {

    private File file;

    public ProfilerIndex(File file) {
        this.file = file;
    }

    public ArrayList<ProfilerRequest> getRequests() {
        ArrayList<ProfilerRequest> list = new ArrayList<ProfilerRequest>();

        String trennzeichen = ",";

        try {
            BufferedReader in = new BufferedReader(new FileReader(this.file));
            String readString;
            while ((readString = in.readLine()) != null) {
                list.add(new ProfilerRequest(readString.split(trennzeichen), this));
            }

            in.close();
        } catch (IOException e) {

        }


        return list;
    }

    public String getPath(ProfilerRequest profilerRequest) {
        String[] hash = profilerRequest.getHash().split("(?<=\\G.{2})");

        return hash[2] + "/" + hash[1] + "/" + profilerRequest.getHash();

    }

    @Nullable
    public File getFile(ProfilerRequest profilerRequest) {
        String path = this.getPath(profilerRequest);

        File file = new File(this.file.getParentFile().getAbsolutePath() + "/" + path);

        if(!file.exists()) {
            return null;
        }

        return file;
    }

    @Nullable
    public ProfilerRequest getRequestOnHash(String hash) {
        for(ProfilerRequest profilerRequest :this.getRequests()) {
            if(profilerRequest.getHash().equals(hash)) {
                return profilerRequest;
            }
        }

        return null;
    }

    @Nullable
    public String getContent(ProfilerRequest profilerRequest) {
        File file = this.getFile(profilerRequest);
        if(file == null) {
            return  null;
        }

        StringBuilder content = new StringBuilder();

        try {
            BufferedReader in = new BufferedReader(new FileReader(file));
            String str;
            while ((str = in.readLine()) != null) {
                content.append(str).append(System.getProperty("line.separator"));
            }
            in.close();
        } catch (IOException e) {
        }


        return content.toString();
    }

}
