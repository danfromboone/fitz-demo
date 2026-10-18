package dfb.demo;

import dfb.demo.domain.Foo;

import java.util.Set;

public class FooService {

    private final FooRepository fooRepository;

    public FooService(FooRepository fooRepository){
        this.fooRepository = fooRepository;
    }

    public Set<Foo> findAll() {
        return fooRepository.getAll();
    }

}
