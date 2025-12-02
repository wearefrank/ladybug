package org.wearefrank.ladybug.web.springmvc.api;

@RestController
@RequestMapping("/collector")
@RolesAllowed("IbisWebService")
public class CollectorApi {
    @Autowired
    private @Setter CollectorApiImpl delegate;

    @PostMapping(value = "/")
    public ResponseEntity<Void> collectSpans(Span[] trace) {
        delegate.processSpans(trace);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> collectSpansJson(Span[] trace) {
        delegate.processSpans(trace);
        return ResponseEntity.ok().build();
    }

}