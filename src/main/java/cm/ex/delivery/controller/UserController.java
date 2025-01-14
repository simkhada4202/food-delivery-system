package cm.ex.delivery.controller;

import cm.ex.delivery.entity.User;
import cm.ex.delivery.response.BasicResponse;
import cm.ex.delivery.service.UserServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user/account")
public class UserController {

    @Autowired
    private UserServiceImpl userService;

    @PostMapping("/test")
    public ResponseEntity<BasicResponse> test() {
        return ResponseEntity.status(HttpStatusCode.valueOf(418)).body(new BasicResponse("I'm a Tea pot"));
    }

    @GetMapping("/viewProfile")
    public ResponseEntity<User> viewProfile() {
        return ResponseEntity.status(HttpStatusCode.valueOf(200)).body(userService.userInfo());
    }

    @PostMapping("/updateProfile")
    public ResponseEntity<BasicResponse> updateProfile(@RequestBody User user) {
        BasicResponse basicResponse = userService.updateUser(user);
        return ResponseEntity.status(HttpStatusCode.valueOf(basicResponse.getCode())).body(basicResponse);
    }

    @PostMapping("/deleteProfile")
    public ResponseEntity<BasicResponse> deleteProfile() {
        BasicResponse basicResponse = userService.deleteUser();
        return ResponseEntity.status(HttpStatusCode.valueOf(basicResponse.getCode())).body(basicResponse);
    }

    @PostMapping("/requestAuthorityModerator")
    public ResponseEntity<BasicResponse> requestAuthorityModerator() {
        return ResponseEntity.status(HttpStatusCode.valueOf(418)).body(new BasicResponse("I'm a Tea pot"));
    }

    @PostMapping("/requestAuthorityStaff")
    public ResponseEntity<BasicResponse> requestAuthorityStaff(@RequestParam String restaurant) {
        return ResponseEntity.status(HttpStatusCode.valueOf(418)).body(new BasicResponse("I'm a Tea pot"));
    }

    @PostMapping("/requestAuthorityDeliveryPerson")
    public ResponseEntity<BasicResponse> requestAuthorityDeliveryPerson() {
        return ResponseEntity.status(HttpStatusCode.valueOf(418)).body(new BasicResponse("I'm a Tea pot"));
    }
}
// account & request role

