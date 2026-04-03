import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'

// Example test to verify setup works
describe('Test Setup', () => {
  it('should render a simple component', () => {
    render(<div data-testid="test">Hello World</div>)
    expect(screen.getByTestId('test')).toHaveTextContent('Hello World')
  })

  it('should have working assertions', () => {
    expect(1 + 1).toBe(2)
    expect({ a: 1 }).toEqual({ a: 1 })
    expect([1, 2, 3]).toContain(2)
  })
})

// Remove this file and create real tests for your components!
