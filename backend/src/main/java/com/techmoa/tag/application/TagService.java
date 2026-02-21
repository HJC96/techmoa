package com.techmoa.tag.application;

import com.techmoa.tag.domain.Tag;
import com.techmoa.tag.domain.TagRepository;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TagService {

    private final TagRepository tagRepository;

    public TagService(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    @Transactional(readOnly = true)
    public List<Tag> findAll() {
        return tagRepository.findAll().stream()
                .sorted(Comparator.comparing(Tag::getName))
                .toList();
    }
}
