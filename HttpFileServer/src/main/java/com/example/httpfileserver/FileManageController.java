package com.example.httpfileserver;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@RestController
public class FileManageController {


    @RequestMapping(value = "/files/{filePath}", method = RequestMethod.GET)
    public String getFilesDirectory(@PathVariable("filePath") String filePath) {
        File file = new File("/home/lollipop", filePath);
        System.out.println(file.getAbsolutePath());
        File[] files = file.listFiles();
        List<String> filesList = new ArrayList<>();

        for (File f : files) {
            if (f.isDirectory()) {
                filesList.add(String.format("<a hrf=\"%s\">%s</a>", f.getAbsolutePath(), f.getName()));
            }
            else {
                filesList.add(f.getName());
            }
        }
        return "FileTree";
    }

    @RequestMapping(value = "/files", method = RequestMethod.GET)
    public ModelAndView getRootDirectory() {
        File file = new File("/home/lollipop/");
        System.out.println(file.getAbsolutePath());
        File[] files = file.listFiles();
        List<String> filesList = new ArrayList<>();

        for (File f : files) {
            if (f.isDirectory()) {
                filesList.add(String.format("<a href=\"%s\">%s</a>", f.getAbsolutePath(), f.getName()));
            } else {
                filesList.add(f.getName());
            }
        }
        ModelAndView modelAndView = new ModelAndView("FileTree");
        modelAndView.addObject("list", filesList);
        return modelAndView;
    }
}
