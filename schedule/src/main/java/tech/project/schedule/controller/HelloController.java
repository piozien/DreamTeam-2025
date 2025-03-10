package tech.project.schedule.controller;

import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

@RestController
@AllArgsConstructor
public class HelloController {
    @GetMapping("/hello")
    public String test() {
        return "Hello World!";
    }

    @GetMapping("/")
    public RedirectView redirectToHello() {
        return new RedirectView("/hello");
    }
}
