package dfb.demo;

import dfb.demo.domain.Goo;

import java.util.Set;

public class GooService {
    private final GooRepository gooRepository;

    public GooService(GooRepository gooRepository){
        this.gooRepository = gooRepository;
    }

    public Set<Goo> findAll() {
        return gooRepository.getAll();
    }
}
