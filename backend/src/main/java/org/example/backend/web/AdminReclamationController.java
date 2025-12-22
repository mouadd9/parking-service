package org.example.backend.web;

import org.example.backend.DTO.ReclamationResponseDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/reclamations")
public class AdminReclamationController {

    // this will intercept requests for responding to reclamations
 //   @PostMapping("/reply")
  //  public ResponseEntity<String> replyToReclamation(@RequestBody ReclamationResponseDTO request) {
        // for future
        // we should create a service for this and inject it here ....
   // }
}
